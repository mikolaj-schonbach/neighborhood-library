package com.example.neighborhood_library.repo;

import com.example.neighborhood_library.domain.Copy;
import com.example.neighborhood_library.domain.CopyStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface CopyRepository extends JpaRepository<Copy, Long> {

    long countByPublicationIdAndStatus(Long publicationId, CopyStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Copy> findFirstByPublicationIdAndStatusOrderByIdAsc(Long publicationId, CopyStatus status);

    boolean existsByPublicationIdAndStatusAndDeletedAtIsNull(Long publicationId, CopyStatus status);

    @Query(value = """
        SELECT c.*
        FROM copies c
        WHERE c.publication_id = :publicationId
          AND c.status = 'AVAILABLE'
          AND c.deleted_at IS NULL
        ORDER BY c.id
        FOR UPDATE SKIP LOCKED
        LIMIT 1
        """, nativeQuery = true)
    Optional<Copy> lockFirstAvailableCopy(@Param("publicationId") Long publicationId);
}
