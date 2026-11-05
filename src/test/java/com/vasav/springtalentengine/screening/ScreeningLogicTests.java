package com.vasav.springtalentengine.screening;

import com.vasav.springtalentengine.jobs.JobRubric;
import com.vasav.springtalentengine.jobs.RequirementType;
import com.vasav.springtalentengine.jobs.SkillDictionary;
import com.vasav.springtalentengine.screening.KeywordGate.KeywordMatch;
import com.vasav.springtalentengine.screening.ScoreCardVerifier.Verification;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ScreeningLogicTests {

    private static final String RESUME = """
            Senior Backend Engineer
            Built payment APIs with Spring Boot and Java 21 for 4 years.
            Deployed services on k8s and wrote CI pipelines.
            """;

    private final SkillDictionary dictionary = new SkillDictionary(Map.of(
            "k8s", "Kubernetes",
            "kubernetes", "Kubernetes",
            "springboot", "Spring Boot"));

    private final List<JobRubric.Item> items = List.of(
            new JobRubric.Item("Java", RequirementType.MUST, 5, 3),
            new JobRubric.Item("Kubernetes", RequirementType.MUST, 3, null),
            new JobRubric.Item("Kafka", RequirementType.NICE, 2, null));

    @Test
    void keywordGateMatchesSynonymsAndScoresByWeight() {
        KeywordMatch match = KeywordGate.evaluate(items, RESUME, dictionary);

        assertThat(match.matched()).containsExactly("Java", "Kubernetes");
        assertThat(match.missingMust()).isEmpty();
        assertThat(match.passes()).isTrue();
        assertThat(match.score()).isEqualTo(80);
    }

    @Test
    void keywordGateFailsWhenMustSkillIsMissing() {
        KeywordMatch match = KeywordGate.evaluate(items, "Python developer", dictionary);

        assertThat(match.passes()).isFalse();
        assertThat(match.missingMust()).containsExactly("Java", "Kubernetes");
    }

    @Test
    void keywordGateDoesNotMatchInsideOtherWords() {
        List<JobRubric.Item> javaOnly = List.of(new JobRubric.Item("Java", RequirementType.MUST, 5, null));

        assertThat(KeywordGate.evaluate(javaOnly, "Expert in JavaScript", dictionary).passes()).isFalse();
    }

    @Test
    void verifierKeepsQuotedEvidenceAndZeroesInventedEvidence() {
        AgentScoreCard card = new AgentScoreCard(List.of(
                new AgentScoreCard.Criterion("java", 5, "Built payment APIs with Spring Boot and Java 21 for 4 years."),
                new AgentScoreCard.Criterion("Kubernetes", 4, "Ran Kubernetes clusters in production"),
                new AgentScoreCard.Criterion("Kafka", 0, "")), List.of(), List.of(), Recommendation.YES, "Solid backend");

        Verification verification = ScoreCardVerifier.verify(card, items, RESUME);

        assertThat(verification.unsupportedSkills()).containsExactly("Kubernetes");
        assertThat(verification.criteria()).extracting(ScreeningReport.CriterionResult::score).containsExactly(5, 0, 0);
        assertThat(verification.criteria()).extracting(ScreeningReport.CriterionResult::verified).containsExactly(true, false, true);
    }

    @Test
    void verifierAcceptsQuotesWithEllipsisAndDifferentCase() {
        String normalized = ScoreCardVerifier.normalize(RESUME);

        assertThat(ScoreCardVerifier.isQuoted("built payment APIs ... on K8S", normalized)).isTrue();
        assertThat(ScoreCardVerifier.isQuoted("", normalized)).isFalse();
    }

    @Test
    void scoresCombineKeywordAndAiParts() {
        List<ScreeningReport.CriterionResult> criteria = List.of(
                new ScreeningReport.CriterionResult("Java", 5, "", true),
                new ScreeningReport.CriterionResult("Kubernetes", 0, "", false),
                new ScreeningReport.CriterionResult("Kafka", 0, "", true));

        int aiScore = ScoreCardVerifier.aiScore(items, criteria);

        assertThat(aiScore).isEqualTo(50);
        assertThat(ScreeningOutcome.finalScore(80, aiScore)).isEqualTo(59);
    }
}
