package com.vasav.springtalentengine.screening;

import java.util.List;
import java.util.Map;

final class RankingAgreement {

    private RankingAgreement() {
    }

    static double pairwise(List<String> expectedOrder, Map<String, Integer> scores) {
        int pairs = 0;
        double agreeing = 0;
        for (int better = 0; better < expectedOrder.size(); better++) {
            for (int worse = better + 1; worse < expectedOrder.size(); worse++) {
                int betterScore = scores.get(expectedOrder.get(better));
                int worseScore = scores.get(expectedOrder.get(worse));
                pairs++;
                if (betterScore > worseScore) {
                    agreeing += 1;
                } else if (betterScore == worseScore) {
                    agreeing += 0.5;
                }
            }
        }
        return pairs == 0 ? 1.0 : agreeing / pairs;
    }
}
