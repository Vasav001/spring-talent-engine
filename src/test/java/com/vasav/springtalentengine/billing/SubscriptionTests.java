package com.vasav.springtalentengine.billing;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class SubscriptionTests {

    private final Instant now = Instant.parse("2026-11-01T10:00:00Z");

    @Test
    void newTenantStartsOnFreeWithFiftyCredits() {
        Subscription subscription = new Subscription(UUID.randomUUID(), now);

        assertThat(subscription.getPlan()).isEqualTo(Plan.FREE);
        assertThat(subscription.getCreditsRemaining()).isEqualTo(50);
        assertThat(subscription.getPeriodEnd()).isEqualTo(now.plus(Duration.ofDays(30)));
    }

    @Test
    void reservesAndRefundsCredits() {
        Subscription subscription = new Subscription(UUID.randomUUID(), now);

        assertThat(subscription.hasCredits(50)).isTrue();
        assertThat(subscription.hasCredits(51)).isFalse();
        subscription.reserve(10);
        subscription.refund(2);

        assertThat(subscription.getCreditsRemaining()).isEqualTo(42);
    }

    @Test
    void byokPlanHasUnlimitedCredits() {
        Subscription subscription = new Subscription(UUID.randomUUID(), now);
        subscription.change(Plan.BYOK, now);

        assertThat(subscription.unlimited()).isTrue();
        assertThat(subscription.hasCredits(10_000)).isTrue();
    }

    @Test
    void expiresOnlyAfterThePeriodEnds() {
        Subscription subscription = new Subscription(UUID.randomUUID(), now);

        assertThat(subscription.expired(now.plus(Duration.ofDays(30)))).isFalse();
        assertThat(subscription.expired(now.plus(Duration.ofDays(30)).plusSeconds(1))).isTrue();
    }
}
