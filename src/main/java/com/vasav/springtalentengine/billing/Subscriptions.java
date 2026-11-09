package com.vasav.springtalentengine.billing;

import com.vasav.springtalentengine.shared.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Component
@RequiredArgsConstructor
class Subscriptions {

    private final SubscriptionRepository subscriptionRepository;
    private final LedgerRepository ledgerRepository;

    @Transactional(propagation = Propagation.MANDATORY)
    Subscription current() {
        UUID tenantId = TenantContext.require();
        Subscription subscription = subscriptionRepository.findLockedByTenantId(tenantId)
                .orElseThrow(() -> new IllegalStateException("Tenant " + tenantId + " has no subscription"));
        if (subscription.expired(Instant.now())) {
            change(subscription, Plan.FREE);
        }
        return subscription;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    void start(UUID tenantId) {
        grant(subscriptionRepository.save(new Subscription(tenantId, Instant.now())));
    }

    @Transactional(propagation = Propagation.MANDATORY)
    void change(Subscription subscription, Plan plan) {
        subscription.change(plan, Instant.now());
        grant(subscription);
    }

    private void grant(Subscription subscription) {
        int credits = subscription.unlimited() ? 0 : subscription.getCreditsRemaining();
        ledgerRepository.save(new LedgerEntry(subscription.getTenantId(), LedgerType.GRANT, credits, null, null,
                subscription.getPlan() + " plan"));
    }
}
