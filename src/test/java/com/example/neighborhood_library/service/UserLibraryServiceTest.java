package com.example.neighborhood_library.service;

import com.example.neighborhood_library.repo.LoanRepository;
import com.example.neighborhood_library.repo.ReservationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserLibraryServiceTest {

    @Mock
    private ReservationRepository reservationRepository;

    @Mock
    private LoanRepository loanRepository;

    @InjectMocks
    private UserLibraryService service;

    @Test
    void userReservations_ShouldCallRepositoryWithCorrectUserId() {
        // given
        Long userId = 100L;
        when(reservationRepository.findByUserIdOrderByReservedAtDesc(userId))
                .thenReturn(Collections.emptyList());

        // when
        service.userReservations(userId);

        // then
        verify(reservationRepository).findByUserIdOrderByReservedAtDesc(userId);
    }

    @Test
    void userActiveLoans_ShouldCallRepositoryWithCorrectUserId() {
        // given
        Long userId = 200L;
        when(loanRepository.findByUserIdAndReturnedAtIsNullOrderByLoanedAtDesc(userId))
                .thenReturn(Collections.emptyList());

        // when
        service.userActiveLoans(userId);

        // then
        verify(loanRepository).findByUserIdAndReturnedAtIsNullOrderByLoanedAtDesc(userId);
    }
}
