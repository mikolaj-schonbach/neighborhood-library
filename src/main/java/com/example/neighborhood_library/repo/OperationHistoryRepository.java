package com.example.neighborhood_library.repo;

import com.example.neighborhood_library.domain.OperationHistory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OperationHistoryRepository extends JpaRepository<OperationHistory, Long> {
}