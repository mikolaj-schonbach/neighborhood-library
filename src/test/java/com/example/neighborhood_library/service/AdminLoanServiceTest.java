package com.example.neighborhood_library.service;

import com.example.neighborhood_library.domain.Loan;
import com.example.neighborhood_library.repo.LoanRepository;
import com.example.neighborhood_library.support.NotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
class AdminLoanServiceTest {

    @Mock
    private LoanRepository loanRepository;

    @InjectMocks
    private AdminLoanService service;

    @Test
    void activeLoans_ShouldCallRepository() {
        // given
        Pageable pageable = Pageable.unpaged();
        when(loanRepository.findByReturnedAtIsNullOrderByDueDateAsc(pageable))
                .thenReturn(Page.empty());

        // when
        service.activeLoans(pageable);

        // then
        verify(loanRepository).findByReturnedAtIsNullOrderByDueDateAsc(pageable);
    }

    @Test
    void acceptReturn_ShouldSetReturnedAt_WhenNotReturnedYet() {
        // given
        Long loanId = 1L;
        Loan loan = new Loan();
        loan.setReturnedAt(null);

        when(loanRepository.findById(loanId)).thenReturn(Optional.of(loan));

        // when
        service.acceptReturn(loanId);

        // then
        assertNotNull(loan.getReturnedAt());
        verify(loanRepository).save(loan);
    }

    @Test
    void acceptReturn_ShouldDoNothing_WhenAlreadyReturned() {
        // given
        Long loanId = 1L;
        Loan loan = new Loan();
        loan.setReturnedAt(OffsetDateTime.now()); // Already returned

        when(loanRepository.findById(loanId)).thenReturn(Optional.of(loan));

        // when
        service.acceptReturn(loanId);

        // then
        // returnedAt shouldn't change (strictly speaking, we rely on it being non-null)
        verify(loanRepository, never()).save(any());
    }

    @Test
    void acceptReturn_ShouldThrowException_WhenLoanNotFound() {
        // given
        Long loanId = 99L;
        when(loanRepository.findById(loanId)).thenReturn(Optional.empty());

        // when & then
        assertThrows(NotFoundException.class, () -> service.acceptReturn(loanId));
        verify(loanRepository, never()).save(any());
    }
}