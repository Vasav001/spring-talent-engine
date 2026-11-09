package com.vasav.springtalentengine.screening;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class RankingAgreementTests {

    private final List<String> expected = List.of("a", "b", "c");

    @Test
    void sameOrderAgreesFully() {
        assertThat(RankingAgreement.pairwise(expected, Map.of("a", 90, "b", 60, "c", 10))).isEqualTo(1.0);
    }

    @Test
    void reversedOrderAgreesNotAtAll() {
        assertThat(RankingAgreement.pairwise(expected, Map.of("a", 10, "b", 60, "c", 90))).isEqualTo(0.0);
    }

    @Test
    void oneSwapLosesOnePair() {
        assertThat(RankingAgreement.pairwise(expected, Map.of("a", 60, "b", 90, "c", 10))).isCloseTo(2.0 / 3, within(0.001));
    }

    @Test
    void tiesCountAsHalf() {
        assertThat(RankingAgreement.pairwise(List.of("a", "b"), Map.of("a", 50, "b", 50))).isEqualTo(0.5);
    }
}
