package com.example.neighborhood_library.service;

import com.example.neighborhood_library.domain.Loan;
import com.example.neighborhood_library.domain.Reservation;
import com.example.neighborhood_library.repo.LoanRepository;
import com.example.neighborhood_library.repo.ReservationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class UserLibraryService {

    private final ReservationRepository reservationRepository;
    private final LoanRepository loanRepository;

    public UserLibraryService(ReservationRepository reservationRepository, LoanRepository loanRepository) {
        this.reservationRepository = reservationRepository;
        this.loanRepository = loanRepository;
    }

    @Transactional(readOnly = true)
    public List<Reservation> userReservations(Long userId) {
        return reservationRepository.findByUserIdOrderByReservedAtDesc(userId);
    }

    @Transactional(readOnly = true)
    public List<Loan> userActiveLoans(Long userId) {
        return loanRepository.findByUserIdAndReturnedAtIsNullOrderByLoanedAtDesc(userId);
    }
}
