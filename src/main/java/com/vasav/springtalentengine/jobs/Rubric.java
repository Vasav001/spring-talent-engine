package com.vasav.springtalentengine.jobs;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

final class Rubric {

    static final int MAX_REQUIREMENTS = 20;

    private static final int DEFAULT_WEIGHT = 3;

    private Rubric() {
    }

    static List<RequirementDraft> clean(List<RequirementDraft> drafts, SkillDictionary dictionary) {
        Map<String, RequirementDraft> bySkill = new LinkedHashMap<>();
        for (RequirementDraft draft : drafts) {
            if (draft == null || draft.skill() == null || draft.skill().isBlank() || draft.type() == null) {
                continue;
            }
            RequirementDraft normalized = new RequirementDraft(
                    dictionary.normalize(draft.skill()),
                    draft.type(),
                    draft.weight() == null ? DEFAULT_WEIGHT : Math.clamp(draft.weight(), 1, 5),
                    draft.minYears() == null ? null : Math.clamp(draft.minYears(), 0, 40));
            bySkill.merge(normalized.skill().toLowerCase(Locale.ROOT), normalized, Rubric::stronger);
        }
        return bySkill.values().stream().limit(MAX_REQUIREMENTS).toList();
    }

    private static RequirementDraft stronger(RequirementDraft existing, RequirementDraft candidate) {
        if (existing.type() != candidate.type()) {
            return existing.type() == RequirementType.MUST ? existing : candidate;
        }
        return existing.weight() >= candidate.weight() ? existing : candidate;
    }
}
