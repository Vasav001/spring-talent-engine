package com.vasav.springtalentengine.jobs;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

interface JobRepository extends JpaRepository<Job, UUID> {

    Page<Job> findByTenantId(UUID tenantId, Pageable pageable);

    Page<Job> findByTenantIdAndStatus(UUID tenantId, JobStatus status, Pageable pageable);
}
