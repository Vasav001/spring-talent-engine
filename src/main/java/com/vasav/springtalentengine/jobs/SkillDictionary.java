package com.vasav.springtalentengine.jobs;

import java.util.Locale;
import java.util.Map;

public record SkillDictionary(Map<String, String> synonyms) {

    public String normalize(String skill) {
        String trimmed = skill.trim();
        return synonyms.getOrDefault(trimmed.toLowerCase(Locale.ROOT), trimmed);
    }
}
