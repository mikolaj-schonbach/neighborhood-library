package com.example.neighborhood_library.repo;

import com.example.neighborhood_library.domain.Loan;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface LoanRepository extends JpaRepository<Loan, Long> {

    @EntityGraph(attributePaths = {"copy", "copy.publication", "user", "reservation"})
    List<Loan> findByUserIdAndReturnedAtIsNullOrderByLoanedAtDesc(Long userId);

    @EntityGraph(attributePaths = {"copy", "copy.publication", "user", "reservation"})
    Page<Loan> findByReturnedAtIsNullOrderByDueDateAsc(Pageable pageable);

    @EntityGraph(attributePaths = {"user", "copy", "copy.publication"})
    List<Loan> findByDueDateAndReturnedAtIsNull(LocalDate dueDate);

    // Do dashboardu: licznik przetrzymanych (termin minął < dzisiaj i nie oddano)
    long countByDueDateBeforeAndReturnedAtIsNull(LocalDate date);
}
