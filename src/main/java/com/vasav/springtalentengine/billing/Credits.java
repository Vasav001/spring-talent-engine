package com.vasav.springtalentengine.billing;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class Credits {

    private final Subscriptions subscriptions;
    private final LedgerRepository ledgerRepository;

    @Transactional
    public void reserveForBatch(UUID batchId, int count) {
        reserve(count, batchId, null);
    }

    @Transactional
    public void reserveForResume(UUID resumeId) {
        reserve(1, null, resumeId);
    }

    @Transactional
    public void refund(UUID batchId, UUID resumeId) {
        Subscription subscription = subscriptions.current();
        if (subscription.unlimited()) {
            return;
        }
        subscription.refund(1);
        ledgerRepository.save(new LedgerEntry(subscription.getTenantId(), LedgerType.REFUND, 1, batchId, resumeId,
                "AI screening not used"));
    }

    @Transactional
    public boolean usesOwnKey() {
        return subscriptions.current().getPlan().ownKey();
    }

    @Transactional
    public CreditBalance balance() {
        Subscription subscription = subscriptions.current();
        return new CreditBalance(subscription.getPlan(), subscription.getCreditsRemaining(),
                subscription.getPeriodStart(), subscription.getPeriodEnd());
    }

    private void reserve(int count, UUID batchId, UUID resumeId) {
        Subscription subscription = subscriptions.current();
        if (!subscription.hasCredits(count)) {
            throw new ResponseStatusException(HttpStatus.PAYMENT_REQUIRED,
                    "Not enough credits: %d needed, %d left".formatted(count, subscription.getCreditsRemaining()));
        }
        if (subscription.unlimited()) {
            return;
        }
        subscription.reserve(count);
        ledgerRepository.save(new LedgerEntry(subscription.getTenantId(), LedgerType.RESERVE, -count, batchId, resumeId,
                batchId == null ? "Single screening" : "Batch submitted"));
    }
}
