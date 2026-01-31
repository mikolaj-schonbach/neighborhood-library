package com.example.neighborhood_library.service;

import com.example.neighborhood_library.domain.*;
import com.example.neighborhood_library.repo.LoanRepository;
import com.example.neighborhood_library.repo.MessageRepository;
import com.example.neighborhood_library.repo.ReservationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.OffsetDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminReservationServiceTest {

    @Mock private ReservationRepository reservationRepository;
    @Mock private LoanRepository loanRepository;
    @Mock private MessageRepository messageRepository;

    @InjectMocks
    private AdminReservationService service;

    @Test
    void activeReservations_ShouldCallRepo() {
        // given
        Pageable pageable = Pageable.unpaged();
        when(reservationRepository.findByStatusOrderByReservedAtDesc(ReservationStatus.ACTIVE, pageable))
                .thenReturn(Page.empty());

        // when
        service.activeReservations(pageable);

        // then
        verify(reservationRepository).findByStatusOrderByReservedAtDesc(ReservationStatus.ACTIVE, pageable);
    }

    // --- Cancel ---

    @Test
    void cancelByAdmin_ShouldCancel_WhenActive() {
        // given
        Long resId = 1L;
        Reservation r = new Reservation();
        r.setStatus(ReservationStatus.ACTIVE);

        when(reservationRepository.findById(resId)).thenReturn(Optional.of(r));

        // when
        service.cancelByAdmin(resId);

        // then
        assertEquals(ReservationStatus.CANCELLED_BY_ADMIN, r.getStatus());
        assertNotNull(r.getCancelledAt());
        verify(reservationRepository).save(r);
    }

    @Test
    void cancelByAdmin_ShouldThrowException_WhenNotActive() {
        // given
        Long resId = 1L;
        Reservation r = new Reservation();
        r.setStatus(ReservationStatus.FULFILLED);

        when(reservationRepository.findById(resId)).thenReturn(Optional.of(r));

        // when & then
        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                service.cancelByAdmin(resId)
        );
        assertTrue(ex.getMessage().contains("tylko aktywną"));
        verify(reservationRepository, never()).save(any());
    }

    // --- Issue Loan ---

    @Test
    void issueLoan_ShouldCreateLoan_WhenReservationIsValid() {
        // given
        Long resId = 10L;
        User user = new User();
        Copy copy = new Copy();
        Publication pub = new Publication();
        pub.setTitle("Title");
        copy.setPublication(pub);

        Reservation r = new Reservation();
        r.setStatus(ReservationStatus.ACTIVE);
        r.setUser(user);
        r.setCopy(copy);
        r.setPickupUntil(OffsetDateTime.now().plusDays(1)); // Valid pickup date

        when(reservationRepository.findById(resId)).thenReturn(Optional.of(r));

        // when
        service.issueLoan(resId);

        // then
        ArgumentCaptor<Loan> loanCaptor = ArgumentCaptor.forClass(Loan.class);
        verify(loanRepository).saveAndFlush(loanCaptor.capture());

        Loan savedLoan = loanCaptor.getValue();
        assertEquals(user, savedLoan.getUser());
        assertEquals(copy, savedLoan.getCopy());
        assertEquals(r, savedLoan.getReservation());

        verify(messageRepository).save(any(Message.class));
    }

    @Test
    void issueLoan_ShouldExpireReservation_WhenPickupDatePassed() {
        // given
        Long resId = 10L;
        Reservation r = new Reservation();
        r.setStatus(ReservationStatus.ACTIVE);
        r.setPickupUntil(OffsetDateTime.now().minusDays(1)); // Expired

        when(reservationRepository.findById(resId)).thenReturn(Optional.of(r));

        // when & then
        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                service.issueLoan(resId)
        );
        assertTrue(ex.getMessage().contains("przeterminowana"));

        // Verify state change to EXPIRED
        assertEquals(ReservationStatus.EXPIRED, r.getStatus());
        assertNotNull(r.getExpiredAt());
        verify(reservationRepository).save(r);

        // Ensure no loan created
        verify(loanRepository, never()).saveAndFlush(any());
    }
}
