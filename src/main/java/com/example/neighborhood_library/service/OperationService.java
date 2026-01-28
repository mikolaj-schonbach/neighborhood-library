package com.example.neighborhood_library.service;

import com.example.neighborhood_library.domain.Copy;
import com.example.neighborhood_library.domain.OperationHistory;
import com.example.neighborhood_library.domain.User;
import com.example.neighborhood_library.repo.OperationHistoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OperationService {

    private final OperationHistoryRepository historyRepository;

    public OperationService(OperationHistoryRepository historyRepository) {
        this.historyRepository = historyRepository;
    }

    @Transactional(propagation = Propagation.MANDATORY) // Musi biec w ramach istniejącej transakcji biznesowej
    public void logAction(User actor, User target, String action, Copy copy) {
        OperationHistory entry = new OperationHistory(actor, target, action, copy);
        historyRepository.save(entry);
    }
}