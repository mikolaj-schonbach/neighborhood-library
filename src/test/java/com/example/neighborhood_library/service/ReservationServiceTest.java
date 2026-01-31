package com.example.neighborhood_library.service;

import com.example.neighborhood_library.domain.*;
import com.example.neighborhood_library.repo.CopyRepository;
import com.example.neighborhood_library.repo.ReservationRepository;
import com.example.neighborhood_library.repo.UserRepository;
import com.example.neighborhood_library.support.NotFoundException;
import com.example.neighborhood_library.support.ReservationBlockedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReservationServiceTest {

    @Mock private ReservationRepository reservationRepository;
    @Mock private CopyRepository copyRepository;
    @Mock private UserRepository userRepository;
    @Mock private OperationService operationService;

    private final Instant fixedInstant = Instant.parse("2024-06-01T10:00:00Z");
    private final Clock fixedClock = Clock.fixed(fixedInstant, ZoneId.systemDefault());

    private ReservationService service;

    @BeforeEach
    void setUp() {
        service = new ReservationService(
                reservationRepository,
                copyRepository,
                userRepository,
                fixedClock,
                operationService
        );
    }

    // --- Reserve Publication ---

    @Test
    void reservePublication_ShouldCreateReservation_WhenCopyIsAvailable() {
        // given
        Long pubId = 100L;
        Long userId = 1L;

        Copy availableCopy = new Copy();
        User user = new User();

        when(copyRepository.findFirstByPublicationIdAndStatusOrderByIdAsc(pubId, CopyStatus.AVAILABLE))
                .thenReturn(Optional.of(availableCopy));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        // Mock save returning object
        when(reservationRepository.save(any(Reservation.class))).thenAnswer(i -> i.getArgument(0));

        // when
        Reservation result = service.reservePublication(pubId, userId);

        // then
        assertNotNull(result);
        assertEquals(user, result.getUser());
        assertEquals(availableCopy, result.getCopy());
        assertEquals(ReservationStatus.ACTIVE, result.getStatus());

        // Check dates from clock
        assertNotNull(result.getReservedAt());
        assertNotNull(result.getPickupUntil());
        assertTrue(result.getPickupUntil().isAfter(result.getReservedAt()));

        verify(operationService).logAction(user, user, "RESERVATION_CREATED", availableCopy);
    }

    @Test
    void reservePublication_ShouldThrowException_WhenNoCopyAvailable() {
        // given
        Long pubId = 100L;
        Long userId = 1L;

        when(copyRepository.findFirstByPublicationIdAndStatusOrderByIdAsc(pubId, CopyStatus.AVAILABLE))
                .thenReturn(Optional.empty());

        // when & then
        ReservationBlockedException ex = assertThrows(ReservationBlockedException.class, () ->
                service.reservePublication(pubId, userId)
        );
        assertEquals("Brak dostępnych egzemplarzy do rezerwacji.", ex.getMessage());

        verify(reservationRepository, never()).save(any());
    }

    @Test
    void reservePublication_ShouldHandleLimitExceededExceptionFromDb() {
        // given
        Long pubId = 100L;
        Long userId = 1L;
        Copy copy = new Copy();
        User user = new User();

        when(copyRepository.findFirstByPublicationIdAndStatusOrderByIdAsc(pubId, CopyStatus.AVAILABLE))
                .thenReturn(Optional.of(copy));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        // Simulate DB trigger exception
        doThrow(new DataIntegrityViolationException("bla bla limit exceeded bla"))
                .when(reservationRepository).save(any(Reservation.class));

        // when & then
        ReservationBlockedException ex = assertThrows(ReservationBlockedException.class, () ->
                service.reservePublication(pubId, userId)
        );
        assertTrue(ex.getMessage().contains("Limit przekroczony"));
    }

    @Test
    void reservePublication_ShouldHandleNotActiveUserExceptionFromDb() {
        // given
        Long pubId = 100L;
        Long userId = 1L;
        when(copyRepository.findFirstByPublicationIdAndStatusOrderByIdAsc(pubId, CopyStatus.AVAILABLE))
                .thenReturn(Optional.of(new Copy()));
        when(userRepository.findById(userId)).thenReturn(Optional.of(new User()));

        doThrow(new DataIntegrityViolationException("User is not active"))
                .when(reservationRepository).save(any());

        // when & then
        ReservationBlockedException ex = assertThrows(ReservationBlockedException.class, () ->
                service.reservePublication(pubId, userId)
        );
        assertTrue(ex.getMessage().contains("nie jest aktywne"));
    }

    // --- Cancel By User ---

    @Test
    void cancelByUser_ShouldCancel_WhenReservationIsActive() {
        // given
        Long resId = 55L;
        Long userId = 1L;
        User user = new User();
        Reservation r = new Reservation();
        r.setStatus(ReservationStatus.ACTIVE);
        r.setUser(user);
        r.setCopy(new Copy());

        when(reservationRepository.findByIdAndUserId(resId, userId)).thenReturn(Optional.of(r));

        // when
        service.cancelByUser(resId, userId);

        // then
        assertEquals(ReservationStatus.CANCELLED_BY_USER, r.getStatus());
        assertNotNull(r.getCancelledAt());
        verify(reservationRepository).save(r);
        verify(operationService).logAction(user, user, "RESERVATION_CANCELLED", r.getCopy());
    }

    @Test
    void cancelByUser_ShouldThrowException_WhenReservationNotActive() {
        // given
        Long resId = 55L;
        Long userId = 1L;
        Reservation r = new Reservation();
        r.setStatus(ReservationStatus.FULFILLED); // e.g. Loaned

        when(reservationRepository.findByIdAndUserId(resId, userId)).thenReturn(Optional.of(r));

        // when & then
        ReservationBlockedException ex = assertThrows(ReservationBlockedException.class, () ->
                service.cancelByUser(resId, userId)
        );
        assertTrue(ex.getMessage().contains("Nie można anulować"));
        verify(reservationRepository, never()).save(any());
    }

    @Test
    void cancelByUser_ShouldThrowNotFound_WhenReservationDoesNotBelongToUser() {
        // given
        Long resId = 55L;
        Long userId = 1L;

        when(reservationRepository.findByIdAndUserId(resId, userId)).thenReturn(Optional.empty());

        // when & then
        assertThrows(NotFoundException.class, () -> service.cancelByUser(resId, userId));
    }
}
