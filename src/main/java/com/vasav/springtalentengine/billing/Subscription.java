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

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "subscriptions")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
class Subscription {

    static final Duration PERIOD = Duration.ofDays(30);

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private UUID tenantId;

    @Enumerated(EnumType.STRING)
    private Plan plan;

    private Instant periodStart;

    private Instant periodEnd;

    private Integer creditsRemaining;

    Subscription(UUID tenantId, Instant now) {
        this.tenantId = tenantId;
        change(Plan.FREE, now);
    }

    void change(Plan plan, Instant now) {
        this.plan = plan;
        this.periodStart = now;
        this.periodEnd = now.plus(PERIOD);
        this.creditsRemaining = plan.monthlyCredits();
    }

    boolean expired(Instant now) {
        return now.isAfter(periodEnd);
    }

    boolean unlimited() {
        return creditsRemaining == null;
    }

    boolean hasCredits(int count) {
        return unlimited() || creditsRemaining >= count;
    }

    void reserve(int count) {
        creditsRemaining -= count;
    }

    void refund(int count) {
        creditsRemaining += count;
    }
}
