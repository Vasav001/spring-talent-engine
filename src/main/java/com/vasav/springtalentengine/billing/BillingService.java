package com.vasav.springtalentengine.billing;

import com.vasav.springtalentengine.shared.CurrentUser;
import com.vasav.springtalentengine.shared.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
class BillingService {

    private final Subscriptions subscriptions;
    private final PaymentRepository paymentRepository;
    private final LedgerRepository ledgerRepository;
    private final PaymentGateway paymentGateway;

    List<PlanDetails> plans() {
        return Arrays.stream(Plan.values()).map(PlanDetails::from).toList();
    }

    @Transactional
    SubscriptionDetails subscription() {
        return SubscriptionDetails.from(subscriptions.current());
    }

    @Transactional
    CheckoutDetails checkout(Plan plan) {
        if (plan == Plan.FREE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "FREE starts automatically when a paid plan ends");
        }
        Payment payment = paymentRepository.save(new Payment(TenantContext.require(), plan, paymentGateway.provider(),
                CurrentUser.requireEmail()));
        return new CheckoutDetails(payment.getId(), plan, payment.getAmountUsd(), payment.getStatus(),
                paymentGateway.checkoutUrl(payment));
    }

    @Transactional
    PaymentDetails completeFakePayment(UUID paymentId, boolean succeeded) {
        Payment payment = paymentRepository.findLockedById(paymentId)
                .filter(found -> found.getTenantId().equals(TenantContext.require()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Payment not found"));
        if (payment.getStatus() == PaymentStatus.PENDING) {
            payment.complete(succeeded);
            if (succeeded) {
                subscriptions.change(subscriptions.current(), payment.getPlan());
            }
        }
        return new PaymentDetails(payment.getId(), payment.getPlan(), payment.getAmountUsd(), payment.getStatus(),
                payment.getCompletedAt(), SubscriptionDetails.from(subscriptions.current()));
    }

    @Transactional(readOnly = true)
    Page<LedgerDetails> ledger(Pageable pageable) {
        return ledgerRepository.findByTenantId(TenantContext.require(), pageable).map(LedgerDetails::from);
    }

    record PlanDetails(Plan code, int monthlyPriceUsd, Integer monthlyCredits, boolean ownKey) {

        static PlanDetails from(Plan plan) {
            return new PlanDetails(plan, plan.monthlyPriceUsd(), plan.monthlyCredits(), plan.ownKey());
        }
    }

    record SubscriptionDetails(Plan plan, Integer creditsRemaining, Instant periodStart, Instant periodEnd) {

        static SubscriptionDetails from(Subscription subscription) {
            return new SubscriptionDetails(subscription.getPlan(), subscription.getCreditsRemaining(),
                    subscription.getPeriodStart(), subscription.getPeriodEnd());
        }
    }

    record CheckoutDetails(UUID paymentId, Plan plan, int amountUsd, PaymentStatus status, String checkoutUrl) {
    }

    record PaymentDetails(UUID paymentId, Plan plan, int amountUsd, PaymentStatus status, Instant completedAt,
                          SubscriptionDetails subscription) {
    }

    record LedgerDetails(LedgerType type, int amount, UUID batchId, UUID resumeId, String note, Instant createdAt) {

        static LedgerDetails from(LedgerEntry entry) {
            return new LedgerDetails(entry.getType(), entry.getAmount(), entry.getBatchId(), entry.getResumeId(),
                    entry.getNote(), entry.getCreatedAt());
        }
    }
}
