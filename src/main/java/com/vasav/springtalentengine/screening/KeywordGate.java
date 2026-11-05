package com.vasav.springtalentengine.screening;

import com.vasav.springtalentengine.jobs.JobRubric;
import com.vasav.springtalentengine.jobs.RequirementType;
import com.vasav.springtalentengine.jobs.SkillDictionary;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

final class KeywordGate {

    private KeywordGate() {
    }

    static KeywordMatch evaluate(List<JobRubric.Item> items, String resumeText, SkillDictionary dictionary) {
        String text = resumeText.toLowerCase(Locale.ROOT);
        List<String> matched = new ArrayList<>();
        List<String> missingMust = new ArrayList<>();
        int totalWeight = 0;
        int matchedWeight = 0;
        for (JobRubric.Item item : items) {
            totalWeight += item.weight();
            boolean found = dictionary.searchTermsFor(item.skill()).stream().anyMatch(term -> containsTerm(text, term));
            if (found) {
                matched.add(item.skill());
                matchedWeight += item.weight();
            } else if (item.type() == RequirementType.MUST) {
                missingMust.add(item.skill());
            }
        }
        int score = totalWeight == 0 ? 0 : (int) Math.round(100.0 * matchedWeight / totalWeight);
        return new KeywordMatch(score, List.copyOf(matched), List.copyOf(missingMust));
    }

    private static boolean containsTerm(String text, String term) {
        return Pattern.compile("(?<![a-z0-9])" + Pattern.quote(term) + "(?![a-z0-9])").matcher(text).find();
    }

    record KeywordMatch(int score, List<String> matched, List<String> missingMust) {

        boolean passes() {
            return missingMust.isEmpty();
        }
    }
}
