package com.vasav.springtalentengine.ingestion;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

interface ResumeRepository extends JpaRepository<Resume, UUID> {

    boolean existsByJobIdAndContentHash(UUID jobId, String contentHash);

    List<Resume> findByBatchIdOrderByFileName(UUID batchId);

    @Modifying
    @Query("""
            update Resume r set r.status = :status, r.error = :error
            where r.id = :id and r.status = com.vasav.springtalentengine.ingestion.ResumeStatus.QUEUED
            """)
    int finishQueued(@Param("id") UUID id, @Param("status") ResumeStatus status, @Param("error") String error);
}
