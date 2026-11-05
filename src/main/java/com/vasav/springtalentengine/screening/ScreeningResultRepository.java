package com.vasav.springtalentengine.screening;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

interface ScreeningResultRepository extends JpaRepository<ScreeningResult, UUID> {

    Optional<ScreeningResult> findByResumeId(UUID resumeId);

    Page<ScreeningResult> findByTenantIdAndJobId(UUID tenantId, UUID jobId, Pageable pageable);

    Page<ScreeningResult> findByTenantIdAndJobIdAndStatus(UUID tenantId, UUID jobId, ScreeningStatus status, Pageable pageable);
}
