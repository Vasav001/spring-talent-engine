package com.vasav.springtalentengine.billing;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

interface LedgerRepository extends JpaRepository<LedgerEntry, UUID> {

    Page<LedgerEntry> findByTenantId(UUID tenantId, Pageable pageable);
}
