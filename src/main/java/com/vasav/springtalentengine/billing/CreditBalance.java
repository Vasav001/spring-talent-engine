package com.vasav.springtalentengine.billing;

import java.time.Instant;

public record CreditBalance(Plan plan, Integer creditsRemaining, Instant periodStart, Instant periodEnd) {
}
