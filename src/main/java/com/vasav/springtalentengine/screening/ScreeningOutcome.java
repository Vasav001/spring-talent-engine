package com.vasav.springtalentengine.screening;

import com.vasav.springtalentengine.jobs.JobRubric;
import com.vasav.springtalentengine.screening.KeywordGate.KeywordMatch;
import com.vasav.springtalentengine.screening.ScreeningAgent.AgentResult;

import java.util.List;

record ScreeningOutcome(ScreeningStatus status,
                        int keywordScore,
                        Integer aiScore,
                        int finalScore,
                        Recommendation recommendation,
                        ScreeningReport report,
                        String error,
                        String model,
                        int tokensIn,
                        int tokensOut) {

    static final double KEYWORD_WEIGHT = 0.3;

    static ScreeningOutcome lowMatch(KeywordMatch keywords) {
        return new ScreeningOutcome(ScreeningStatus.LOW_MATCH, keywords.score(), null, finalScore(keywords.score(), 0),
                Recommendation.NO, keywordsOnly(keywords), null, null, 0, 0);
    }

    static ScreeningOutcome failed(KeywordMatch keywords, String error) {
        return new ScreeningOutcome(ScreeningStatus.FAILED, keywords.score(), null, 0,
                null, keywordsOnly(keywords), error, null, 0, 0);
    }

    static ScreeningOutcome screened(KeywordMatch keywords, List<JobRubric.Item> items, AgentResult agent) {
        int aiScore = ScoreCardVerifier.aiScore(items, agent.verification().criteria());
        ScreeningReport report = new ScreeningReport(keywords.matched(), keywords.missingMust(),
                agent.verification().criteria(),
                orEmpty(agent.card().strengths()),
                orEmpty(agent.card().gaps()),
                agent.card().summary());
        return new ScreeningOutcome(ScreeningStatus.SCREENED, keywords.score(), aiScore, finalScore(keywords.score(), aiScore),
                agent.card().recommendation(), report, null, agent.model(), agent.tokensIn(), agent.tokensOut());
    }

    static int finalScore(int keywordScore, int aiScore) {
        return (int) Math.round(KEYWORD_WEIGHT * keywordScore + (1 - KEYWORD_WEIGHT) * aiScore);
    }

    private static ScreeningReport keywordsOnly(KeywordMatch keywords) {
        return new ScreeningReport(keywords.matched(), keywords.missingMust(), List.of(), List.of(), List.of(), null);
    }

    private static List<String> orEmpty(List<String> values) {
        return values == null ? List.of() : values;
    }
}
