package com.vasav.springtalentengine.tenant;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

interface TenantRepository extends JpaRepository<Tenant, UUID> {
}
