package com.vasav.springtalentengine.billing;

import org.springframework.stereotype.Component;

@Component
class FakePaymentGateway implements PaymentGateway {

    @Override
    public String provider() {
        return "FAKE";
    }

    @Override
    public String checkoutUrl(Payment payment) {
        return "/billing/fake-gateway/" + payment.getId();
    }
}
