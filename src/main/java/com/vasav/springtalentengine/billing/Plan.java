package com.vasav.springtalentengine.billing;

public enum Plan {
    FREE(0, 50, false),
    PRO(29, 1000, false),
    BYOK(9, null, true);

    private final int monthlyPriceUsd;
    private final Integer monthlyCredits;
    private final boolean ownKey;

    Plan(int monthlyPriceUsd, Integer monthlyCredits, boolean ownKey) {
        this.monthlyPriceUsd = monthlyPriceUsd;
        this.monthlyCredits = monthlyCredits;
        this.ownKey = ownKey;
    }

    public int monthlyPriceUsd() {
        return monthlyPriceUsd;
    }

    public Integer monthlyCredits() {
        return monthlyCredits;
    }

    public boolean ownKey() {
        return ownKey;
    }
}
