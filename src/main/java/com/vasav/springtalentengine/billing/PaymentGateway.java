package com.vasav.springtalentengine.billing;

interface PaymentGateway {

    String provider();

    String checkoutUrl(Payment payment);
}
