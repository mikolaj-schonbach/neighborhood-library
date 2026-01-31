package com.example.neighborhood_library.service;

import com.example.neighborhood_library.domain.*;
import com.example.neighborhood_library.repo.LoanRepository;
import com.example.neighborhood_library.repo.MessageRepository;
import com.example.neighborhood_library.repo.ReservationRepository;
import com.example.neighborhood_library.support.ReservationBlockedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.*;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminCirculationServiceTest {

    @Mock private ReservationRepository reservationRepository;
    @Mock private LoanRepository loanRepository;
    @Mock private MessageRepository messageRepository;
    @Mock private CurrentUserService currentUserService;
    @Mock private OperationService operationService;

    private AdminCirculationService service;
    private final Instant fixedInstant = Instant.parse("2024-05-01T12:00:00Z");
    private final Clock fixedClock = Clock.fixed(fixedInstant, ZoneId.systemDefault());

    @BeforeEach
    void setUp() {
        // Wstrzykujemy ręcznie, aby użyć prawdziwego Clock.fixed zamiast mocka
        service = new AdminCirculationService(
                reservationRepository,
                loanRepository,
                messageRepository,
                fixedClock,
                currentUserService,
                operationService
        );
    }

    // --- Active Reservations ---

    @Test
    void activeReservations_ShouldCallRepository() {
        // given
        Pageable pageable = Pageable.unpaged();
        when(reservationRepository.findByStatusOrderByReservedAtDesc(ReservationStatus.ACTIVE, pageable))
                .thenReturn(Page.empty());

        // when
        service.activeReservations(pageable);

        // then
        verify(reservationRepository).findByStatusOrderByReservedAtDesc(ReservationStatus.ACTIVE, pageable);
    }

    // --- Cancel Reservation ---

    @Test
    void cancelReservationByAdmin_ShouldCancel_WhenStatusIsActive() {
        // given
        Long reservationId = 10L;
        Reservation r = new Reservation();
        ReflectionTestUtils.setField(r, "id", reservationId);
        r.setStatus(ReservationStatus.ACTIVE);
        r.setUser(new User());
        r.setCopy(new Copy());

        when(reservationRepository.findById(reservationId)).thenReturn(Optional.of(r));
        when(currentUserService.requireCurrentUser()).thenReturn(new User());

        // when
        service.cancelReservationByAdmin(reservationId);

        // then
        assertEquals(ReservationStatus.CANCELLED_BY_ADMIN, r.getStatus());
        assertNotNull(r.getCancelledAt());
        verify(reservationRepository).save(r);
        verify(operationService).logAction(any(), any(), eq("RESERVATION_CANCELLED_BY_ADMIN"), any());
    }

    @Test
    void cancelReservationByAdmin_ShouldThrowException_WhenStatusNotActive() {
        // given
        Long reservationId = 10L;
        Reservation r = new Reservation();
        r.setStatus(ReservationStatus.FULFILLED); // Np. już odebrana

        when(reservationRepository.findById(reservationId)).thenReturn(Optional.of(r));

        // when & then
        assertThrows(ReservationBlockedException.class, () ->
                service.cancelReservationByAdmin(reservationId)
        );
        verify(reservationRepository, never()).save(any());
    }

    // --- Issue Loan ---

    @Test
    void issueLoan_ShouldCreateLoan_WhenReservationIsActiveAndNotExpired() {
        // given
        Long reservationId = 20L;
        User user = new User();
        Copy copy = new Copy();
        Publication pub = new Publication();
        pub.setTitle("Test Title");
        copy.setPublication(pub);
        copy.setInventoryCode("LIB-001");

        Reservation r = new Reservation();
        r.setStatus(ReservationStatus.ACTIVE);
        r.setUser(user);
        r.setCopy(copy);
        // Termin odbioru w przyszłości względem fixedClock
        r.setPickupUntil(OffsetDateTime.now(fixedClock).plusDays(1));

        when(reservationRepository.findById(reservationId)).thenReturn(Optional.of(r));
        when(currentUserService.requireCurrentUser()).thenReturn(new User());

        Loan mockSavedLoan = new Loan();
        mockSavedLoan.setDueDate(LocalDate.now(fixedClock).plusDays(30));
        when(loanRepository.save(any(Loan.class))).thenReturn(mockSavedLoan);

        // when
        service.issueLoan(reservationId);

        // then
        // 1. Sprawdzamy utworzenie wypożyczenia
        ArgumentCaptor<Loan> loanCaptor = ArgumentCaptor.forClass(Loan.class);
        verify(loanRepository).save(loanCaptor.capture());
        Loan capturedLoan = loanCaptor.getValue();
        assertEquals(user, capturedLoan.getUser());
        assertEquals(copy, capturedLoan.getCopy());
        assertEquals(r, capturedLoan.getReservation());
        assertNotNull(capturedLoan.getLoanedAt());

        // 2. Sprawdzamy wysłanie wiadomości
        verify(messageRepository).save(any(Message.class));

        // 3. Sprawdzamy log
        verify(operationService).logAction(any(), eq(user), eq("LOAN_CREATED"), eq(copy));
    }

    @Test
    void issueLoan_ShouldMarkExpired_WhenPickupDatePassed() {
        // given
        Long reservationId = 20L;
        Reservation r = new Reservation();
        r.setStatus(ReservationStatus.ACTIVE);
        // Termin odbioru minął względem fixedClock
        r.setPickupUntil(OffsetDateTime.now(fixedClock).minusSeconds(1));

        when(reservationRepository.findById(reservationId)).thenReturn(Optional.of(r));

        // when & then
        ReservationBlockedException ex = assertThrows(ReservationBlockedException.class, () ->
                service.issueLoan(reservationId)
        );
        assertTrue(ex.getMessage().contains("wygasła"));

        // Weryfikacja zmiany stanu rezerwacji na EXPIRED
        assertEquals(ReservationStatus.EXPIRED, r.getStatus());
        assertNotNull(r.getExpiredAt());
        verify(reservationRepository).save(r);

        // Upewniamy się, że NIE utworzono wypożyczenia
        verify(loanRepository, never()).save(any());
    }

    // --- Accept Return ---

    @Test
    void acceptReturn_ShouldUpdateLoan_WhenNotReturnedYet() {
        // given
        Long loanId = 30L;
        Loan loan = new Loan();
        loan.setReturnedAt(null);
        loan.setUser(new User());
        loan.setCopy(new Copy());

        when(loanRepository.findById(loanId)).thenReturn(Optional.of(loan));
        when(currentUserService.requireCurrentUser()).thenReturn(new User());

        // when
        service.acceptReturn(loanId);

        // then
        assertNotNull(loan.getReturnedAt());
        verify(loanRepository).save(loan);
        verify(operationService).logAction(any(), any(), eq("LOAN_RETURNED"), any());
    }

    @Test
    void acceptReturn_ShouldDoNothing_WhenAlreadyReturned() {
        // given
        Long loanId = 30L;
        Loan loan = new Loan();
        loan.setReturnedAt(OffsetDateTime.now().minusDays(1)); // już zwrócono

        when(loanRepository.findById(loanId)).thenReturn(Optional.of(loan));

        // when
        service.acceptReturn(loanId);

        // then
        verify(loanRepository, never()).save(any()); // nie zapisujemy ponownie
        verifyNoInteractions(operationService);
    }
}
