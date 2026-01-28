package com.example.neighborhood_library.repo;

import com.example.neighborhood_library.domain.Reservation;
import com.example.neighborhood_library.domain.ReservationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    @EntityGraph(attributePaths = {"copy", "copy.publication", "user"})
    List<Reservation> findByUserIdOrderByReservedAtDesc(Long userId);

    @EntityGraph(attributePaths = {"copy", "copy.publication", "user"})
    List<Reservation> findByUserIdAndStatusOrderByReservedAtDesc(Long userId, ReservationStatus status);

    @EntityGraph(attributePaths = {"copy", "copy.publication", "user"})
    Page<Reservation> findByStatusOrderByReservedAtDesc(ReservationStatus status, Pageable pageable);

    Optional<Reservation> findByIdAndUserId(Long id, Long userId);


    @EntityGraph(attributePaths = {"copy", "copy.publication"})
    List<Reservation> findByUserIdAndStatusInOrderByReservedAtDesc(Long userId, Collection<ReservationStatus> statuses);

    @Modifying
    @Query(value = """
        UPDATE reservations
        SET status = 'EXPIRED',
            expired_at = COALESCE(expired_at, now())
        WHERE status = 'ACTIVE'
          AND pickup_until < now()
        """, nativeQuery = true)
    int expireOverdue();

    // Do dashboardu
    long countByStatus(ReservationStatus status);
}
