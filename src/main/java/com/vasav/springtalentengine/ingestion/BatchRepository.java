package com.vasav.springtalentengine.ingestion;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

interface BatchRepository extends JpaRepository<Batch, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select b from Batch b where b.id = :id")
    Optional<Batch> findLockedById(@Param("id") UUID id);

    @Modifying
    @Query(value = """
            UPDATE batches
            SET processed = processed + 1,
                failed = failed + :failed,
                status = CASE WHEN processed + 1 >= total THEN 'COMPLETED' ELSE status END,
                completed_at = CASE WHEN processed + 1 >= total THEN now() ELSE completed_at END
            WHERE id = :id
            """, nativeQuery = true)
    void recordProgress(@Param("id") UUID id, @Param("failed") int failed);
}
