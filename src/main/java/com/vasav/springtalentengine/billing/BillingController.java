package com.vasav.springtalentengine.billing;

import com.vasav.springtalentengine.billing.BillingService.CheckoutDetails;
import com.vasav.springtalentengine.billing.BillingService.LedgerDetails;
import com.vasav.springtalentengine.billing.BillingService.PaymentDetails;
import com.vasav.springtalentengine.billing.BillingService.PlanDetails;
import com.vasav.springtalentengine.billing.BillingService.SubscriptionDetails;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
class BillingController {

    private final BillingService billingService;

    @GetMapping("/plans")
    public List<PlanDetails> plans() {
        return billingService.plans();
    }

    @GetMapping("/billing/subscription")
    @PreAuthorize("hasAuthority('RESULT_READ')")
    public SubscriptionDetails subscription() {
        return billingService.subscription();
    }

    @PostMapping("/billing/checkout")
    @PreAuthorize("hasAuthority('BILLING_MANAGE')")
    public ResponseEntity<CheckoutDetails> checkout(@Valid @RequestBody CheckoutRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(billingService.checkout(request.plan()));
    }

    @PostMapping("/billing/fake-gateway/{paymentId}")
    @PreAuthorize("hasAuthority('BILLING_MANAGE')")
    public PaymentDetails completeFakePayment(@PathVariable UUID paymentId, @Valid @RequestBody FakePaymentRequest request) {
        return billingService.completeFakePayment(paymentId, request.result() == FakeResult.SUCCESS);
    }

    @GetMapping("/billing/ledger")
    @PreAuthorize("hasAuthority('BILLING_MANAGE')")
    public Page<LedgerDetails> ledger(@PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return billingService.ledger(pageable);
    }

    enum FakeResult {
        SUCCESS,
        FAILURE
    }

    record CheckoutRequest(@NotNull Plan plan) {
    }

    record FakePaymentRequest(@NotNull FakeResult result) {
    }
}
