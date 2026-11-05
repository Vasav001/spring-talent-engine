package com.vasav.springtalentengine.ingestion;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

interface ResumeRepository extends JpaRepository<Resume, UUID> {

    boolean existsByJobIdAndContentHash(UUID jobId, String contentHash);
}
