package com.example.neighborhood_library.service;

import com.example.neighborhood_library.domain.*;
import com.example.neighborhood_library.repo.LoanRepository;
import com.example.neighborhood_library.repo.MessageRepository;
import com.example.neighborhood_library.repo.ReservationRepository;
import com.example.neighborhood_library.support.NotFoundException;
import com.example.neighborhood_library.support.ReservationBlockedException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;

@Service
public class AdminCirculationService {

    private final ReservationRepository reservationRepository;
    private final LoanRepository loanRepository;
    private final MessageRepository messageRepository;
    private final Clock clock;

    public AdminCirculationService(
            ReservationRepository reservationRepository,
            LoanRepository loanRepository,
            MessageRepository messageRepository,
            Clock clock
    ) {
        this.reservationRepository = reservationRepository;
        this.loanRepository = loanRepository;
        this.messageRepository = messageRepository;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public Page<Reservation> activeReservations(Pageable pageable) {
        return reservationRepository.findByStatusOrderByReservedAtDesc(ReservationStatus.ACTIVE, pageable);
    }

    @Transactional
    public void cancelReservationByAdmin(Long reservationId) {
        Reservation r = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new NotFoundException("Nie znaleziono rezerwacji."));

        if (r.getStatus() != ReservationStatus.ACTIVE) {
            throw new ReservationBlockedException("Nie można anulować rezerwacji w tym statusie: " + r.getStatus());
        }

        OffsetDateTime now = OffsetDateTime.now(clock);
        r.setStatus(ReservationStatus.CANCELLED_BY_ADMIN);
        r.setCancelledAt(now);
        reservationRepository.save(r);
    }

    @Transactional
    public Loan issueLoan(Long reservationId) {
        Reservation r = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new NotFoundException("Nie znaleziono rezerwacji."));

        if (r.getStatus() != ReservationStatus.ACTIVE) {
            throw new ReservationBlockedException("Nie można wydać: rezerwacja ma status " + r.getStatus());
        }

        OffsetDateTime now = OffsetDateTime.now(clock);
        if (now.isAfter(r.getPickupUntil())) {
            // Zamiast polegać na triggerze (który może skończyć się “0 rows inserted”),
            // oznaczamy jako EXPIRED po stronie aplikacji:
            r.setStatus(ReservationStatus.EXPIRED);
            r.setExpiredAt(now);
            reservationRepository.save(r);
            throw new ReservationBlockedException("Rezerwacja wygasła (minął termin odbioru).");
        }

        Loan loan = new Loan();
        loan.setReservation(r);
        loan.setCopy(r.getCopy());
        loan.setUser(r.getUser());
        loan.setLoanedAt(now);
        loan.setDueDate(LocalDate.now(clock).plusDays(30));

        Loan saved = loanRepository.save(loan); // DB trigger ustawi reservations -> FULFILLED i przeliczy copy.status

        Message m = new Message();
        m.setUser(r.getUser());
        m.setType(MessageType.LOAN_CREATED);
        m.setTitle("Wypożyczenie utworzone");
        m.setBody("Wypożyczono: " + r.getCopy().getPublication().getTitle()
                + " (egz. " + r.getCopy().getInventoryCode() + "). Termin zwrotu: " + saved.getDueDate() + ".");
        m.setCreatedAt(now);
        messageRepository.save(m);

        return saved;
    }

    @Transactional(readOnly = true)
    public Page<Loan> activeLoans(Pageable pageable) {
        return loanRepository.findByReturnedAtIsNullOrderByDueDateAsc(pageable);
    }

    @Transactional
    public void acceptReturn(Long loanId) {
        Loan loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new NotFoundException("Nie znaleziono wypożyczenia."));

        if (loan.getReturnedAt() != null) {
            return;
        }

        loan.setReturnedAt(OffsetDateTime.now(clock));
        loanRepository.save(loan); // DB trigger przeliczy copy.status
    }
}
