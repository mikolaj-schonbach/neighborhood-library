package com.example.neighborhood_library.service;

import com.example.neighborhood_library.domain.*;
import com.example.neighborhood_library.repo.LoanRepository;
import com.example.neighborhood_library.repo.MessageRepository;
import com.example.neighborhood_library.repo.ReservationRepository;
import com.example.neighborhood_library.support.NotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

@Service
public class AdminReservationService {

    private final ReservationRepository reservationRepository;
    private final LoanRepository loanRepository;
    private final MessageRepository messageRepository;

    public AdminReservationService(
            ReservationRepository reservationRepository,
            LoanRepository loanRepository,
            MessageRepository messageRepository
    ) {
        this.reservationRepository = reservationRepository;
        this.loanRepository = loanRepository;
        this.messageRepository = messageRepository;
    }

    public Page<Reservation> activeReservations(Pageable pageable) {
        return reservationRepository.findByStatusOrderByReservedAtDesc(ReservationStatus.ACTIVE, pageable);
    }

    @Transactional
    public void cancelByAdmin(Long reservationId) {
        Reservation r = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new NotFoundException("Rezerwacja nie istnieje"));

        if (r.getStatus() != ReservationStatus.ACTIVE) {
            throw new IllegalStateException("Można anulować tylko aktywną rezerwację");
        }

        r.setStatus(ReservationStatus.CANCELLED_BY_ADMIN);
        r.setCancelledAt(OffsetDateTime.now());
        reservationRepository.save(r);
    }

    @Transactional
    public void issueLoan(Long reservationId) {
        Reservation r = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new NotFoundException("Rezerwacja nie istnieje"));

        if (r.getStatus() != ReservationStatus.ACTIVE) {
            throw new IllegalStateException("Można wydać tylko aktywną rezerwację");
        }

        OffsetDateTime now = OffsetDateTime.now();
        if (now.isAfter(r.getPickupUntil())) {
            // nie dopuszczamy do ścieżki trigger->RETURN NULL (problematyczne w JPA)
            r.setStatus(ReservationStatus.EXPIRED);
            r.setExpiredAt(now);
            reservationRepository.save(r);
            throw new IllegalStateException("Rezerwacja przeterminowana (pickup_until minął)");
        }

        Loan loan = new Loan();
        loan.setReservation(r);
        loan.setUser(r.getUser());
        loan.setCopy(r.getCopy());
        loanRepository.saveAndFlush(loan); // DB trigger ustawi reservation=FULFILLED :contentReference[oaicite:6]{index=6}

        Message msg = new Message();
        msg.setUser(r.getUser());
        msg.setType(MessageType.LOAN_CREATED);
        msg.setTitle("Wypożyczenie utworzone");
        msg.setBody("Wypożyczono: " + r.getCopy().getPublication().getTitle()
                + " (egz. " + r.getCopy().getInventoryCode() + "). Termin zwrotu: " + loan.getDueDate() + ".");
        messageRepository.save(msg);
    }
}
