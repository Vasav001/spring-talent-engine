package com.vasav.springtalentengine.screening;

import com.vasav.springtalentengine.jobs.JobRubric;
import com.vasav.springtalentengine.screening.ScreeningReport.CriterionResult;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

final class ScoreCardVerifier {

    private static final int MIN_QUOTE_PART_LENGTH = 3;

    private ScoreCardVerifier() {
    }

    static Verification verify(AgentScoreCard card, List<JobRubric.Item> items, String resumeText) {
        String resume = normalize(resumeText);
        Map<String, AgentScoreCard.Criterion> bySkill = (card.criteria() == null ? List.<AgentScoreCard.Criterion>of() : card.criteria())
                .stream()
                .filter(Objects::nonNull)
                .filter(criterion -> criterion.skill() != null)
                .collect(Collectors.toMap(criterion -> criterion.skill().strip().toLowerCase(Locale.ROOT), Function.identity(), (first, second) -> first));

        List<CriterionResult> results = new ArrayList<>();
        List<String> unsupported = new ArrayList<>();
        for (JobRubric.Item item : items) {
            AgentScoreCard.Criterion criterion = bySkill.get(item.skill().toLowerCase(Locale.ROOT));
            int score = criterion == null || criterion.score() == null ? 0 : Math.clamp(criterion.score(), 0, 5);
            String evidence = criterion == null || criterion.evidence() == null ? "" : criterion.evidence().strip();
            boolean verified = score == 0 || isQuoted(evidence, resume);
            if (!verified) {
                unsupported.add(item.skill());
            }
            results.add(new CriterionResult(item.skill(), verified ? score : 0, evidence, verified));
        }
        return new Verification(List.copyOf(results), List.copyOf(unsupported));
    }

    static int aiScore(List<JobRubric.Item> items, List<CriterionResult> criteria) {
        int totalWeight = items.stream().mapToInt(JobRubric.Item::weight).sum();
        if (totalWeight == 0) {
            return 0;
        }
        double weighted = 0;
        for (int i = 0; i < items.size(); i++) {
            weighted += criteria.get(i).score() * items.get(i).weight();
        }
        return (int) Math.round(100.0 * weighted / (5.0 * totalWeight));
    }

    static boolean isQuoted(String evidence, String normalizedResume) {
        List<String> parts = Arrays.stream(normalize(evidence).split("\\.\\.\\.|…"))
                .map(String::strip)
                .filter(part -> part.length() >= MIN_QUOTE_PART_LENGTH)
                .toList();
        return !parts.isEmpty() && parts.stream().allMatch(normalizedResume::contains);
    }

    static String normalize(String text) {
        return text.toLowerCase(Locale.ROOT)
                .replaceAll("[\"“”‘’'`•·▪●*|]", " ")
                .replaceAll("[–—]", "-")
                .replaceAll("\\s+", " ")
                .strip();
    }

    record Verification(List<CriterionResult> criteria, List<String> unsupportedSkills) {
    }
}
