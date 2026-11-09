package com.vasav.springtalentengine.billing;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "payments")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private UUID tenantId;

    @Enumerated(EnumType.STRING)
    private Plan plan;

    private int amountUsd;

    @Enumerated(EnumType.STRING)
    private PaymentStatus status;

    private String provider;

    private String createdBy;

    private Instant createdAt;

    private Instant completedAt;

    Payment(UUID tenantId, Plan plan, String provider, String createdBy) {
        this.tenantId = tenantId;
        this.plan = plan;
        this.amountUsd = plan.monthlyPriceUsd();
        this.status = PaymentStatus.PENDING;
        this.provider = provider;
        this.createdBy = createdBy;
        this.createdAt = Instant.now();
    }

    void complete(boolean succeeded) {
        this.status = succeeded ? PaymentStatus.SUCCEEDED : PaymentStatus.FAILED;
        this.completedAt = Instant.now();
    }
}
