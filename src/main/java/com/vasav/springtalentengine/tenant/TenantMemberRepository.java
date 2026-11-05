package com.vasav.springtalentengine.tenant;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface TenantMemberRepository extends JpaRepository<TenantMember, UUID> {

    Optional<TenantMember> findByTenantIdAndEmail(UUID tenantId, String email);

    List<TenantMember> findByEmail(String email);

    Page<TenantMember> findByTenantId(UUID tenantId, Pageable pageable);

    boolean existsByTenantIdAndEmail(UUID tenantId, String email);
}
