package com.example.neighborhood_library.support;

import com.example.neighborhood_library.repo.ReservationRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class ReservationExpiryJob {

    private final ReservationRepository reservationRepository;

    public ReservationExpiryJob(ReservationRepository reservationRepository) {
        this.reservationRepository = reservationRepository;
    }

    @Scheduled(fixedDelayString = "${app.reservations.expire-job-ms:60000}")
    @Transactional
    public void expireOverdueReservations() {
        reservationRepository.expireOverdue();
    }
}
