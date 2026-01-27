package com.example.neighborhood_library.service;

import com.example.neighborhood_library.domain.Loan;
import com.example.neighborhood_library.repo.LoanRepository;
import com.example.neighborhood_library.support.NotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

@Service
public class AdminLoanService {

    private final LoanRepository loanRepository;

    public AdminLoanService(LoanRepository loanRepository) {
        this.loanRepository = loanRepository;
    }

    public Page<Loan> activeLoans(Pageable pageable) {
        return loanRepository.findByReturnedAtIsNullOrderByDueDateAsc(pageable);
    }

    @Transactional
    public void acceptReturn(Long loanId) {
        Loan loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new NotFoundException("Wypożyczenie nie istnieje"));

        if (loan.getReturnedAt() != null) {
            return;
        }
        loan.setReturnedAt(OffsetDateTime.now());
        loanRepository.save(loan);
        // copy.status zaktualizuje się triggerami w DB :contentReference[oaicite:7]{index=7}
    }
}
