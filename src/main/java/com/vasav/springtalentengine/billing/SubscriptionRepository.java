package com.vasav.springtalentengine.billing;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

interface SubscriptionRepository extends JpaRepository<Subscription, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from Subscription s where s.tenantId = :tenantId")
    Optional<Subscription> findLockedByTenantId(@Param("tenantId") UUID tenantId);
}
