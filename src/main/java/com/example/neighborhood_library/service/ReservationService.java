package com.example.neighborhood_library.service;

import com.example.neighborhood_library.domain.*;
import com.example.neighborhood_library.repo.CopyRepository;
import com.example.neighborhood_library.repo.ReservationRepository;
import com.example.neighborhood_library.repo.UserRepository;
import com.example.neighborhood_library.support.NotFoundException;
import com.example.neighborhood_library.support.ReservationBlockedException;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;

@Service
public class ReservationService {

    private final ReservationRepository reservationRepository;
    private final CopyRepository copyRepository;
    private final UserRepository userRepository;
    private final Clock clock;
    private final OperationService operationService;

    // MVP: stałe; jeśli chcesz — przerobimy na properties
    private static final int PICKUP_DAYS = 3;

    public ReservationService(
            ReservationRepository reservationRepository,
            CopyRepository copyRepository,
            UserRepository userRepository,
            Clock clock, OperationService operationService
    ) {
        this.reservationRepository = reservationRepository;
        this.copyRepository = copyRepository;
        this.userRepository = userRepository;
        this.clock = clock;
        this.operationService = operationService;
    }

    @Transactional
    public Reservation reservePublication(Long publicationId, Long userId) {
        Copy copy = copyRepository.findFirstByPublicationIdAndStatusOrderByIdAsc(publicationId, CopyStatus.AVAILABLE)
                .orElseThrow(() -> new ReservationBlockedException("Brak dostępnych egzemplarzy do rezerwacji."));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Nie znaleziono użytkownika."));

        OffsetDateTime now = OffsetDateTime.now(clock);

        Reservation r = new Reservation();
        r.setUser(user);
        r.setCopy(copy);
        r.setReservedAt(now);
        r.setPickupUntil(now.plusDays(PICKUP_DAYS));
        r.setStatus(ReservationStatus.ACTIVE);



        try {
            Reservation saved = reservationRepository.save(r);
            operationService.logAction(user, user, "RESERVATION_CREATED", copy);
            return saved;
        } catch (DataAccessException ex) {
            // DB ma triggery i wyjątki (limit 3, status usera, status kopii) — mapujemy na czytelny komunikat
            String msg = (ex.getMostSpecificCause() != null ? ex.getMostSpecificCause().getMessage() : ex.getMessage());
            if (msg != null && msg.toLowerCase().contains("limit exceeded")) {
                throw new ReservationBlockedException("Limit przekroczony: maksymalnie 3 aktywne sztuki (rezerwacje + wypożyczenia).");
            }
            if (msg != null && msg.toLowerCase().contains("not active")) {
                throw new ReservationBlockedException("Twoje konto nie jest aktywne — rezerwacja niedozwolona.");
            }
            if (msg != null && msg.toLowerCase().contains("not available")) {
                throw new ReservationBlockedException("Ten egzemplarz nie jest już dostępny.");
            }
            throw new ReservationBlockedException("Nie udało się utworzyć rezerwacji.");
        }
    }

    @Transactional
    public void cancelByUser(Long reservationId, Long userId) {
        Reservation r = reservationRepository.findByIdAndUserId(reservationId, userId)
                .orElseThrow(() -> new NotFoundException("Nie znaleziono rezerwacji."));

        if (r.getStatus() != ReservationStatus.ACTIVE) {
            throw new ReservationBlockedException("Nie można anulować rezerwacji w tym statusie: " + r.getStatus());
        }

        OffsetDateTime now = OffsetDateTime.now(clock);
        r.setStatus(ReservationStatus.CANCELLED_BY_USER);
        r.setCancelledAt(now);
        User user = r.getUser();
        reservationRepository.save(r);
        operationService.logAction(user, user, "RESERVATION_CANCELLED", r.getCopy());
    }

    public List<Reservation> myActiveReservations(Long userId) {
        return reservationRepository.findByUserIdAndStatusInOrderByReservedAtDesc(
                userId, List.of(ReservationStatus.ACTIVE)
        );
    }

    public boolean canReserve(Long publicationId) {
        return copyRepository.existsByPublicationIdAndStatusAndDeletedAtIsNull(publicationId, CopyStatus.AVAILABLE);
    }
}
