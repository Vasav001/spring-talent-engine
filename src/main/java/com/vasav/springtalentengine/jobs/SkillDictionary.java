package com.vasav.springtalentengine.jobs;

import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public record SkillDictionary(Map<String, String> synonyms) {

    public String normalize(String skill) {
        String trimmed = skill.trim();
        return synonyms.getOrDefault(trimmed.toLowerCase(Locale.ROOT), trimmed);
    }

    public Set<String> searchTermsFor(String skill) {
        String canonical = normalize(skill);
        return Stream.concat(
                        Stream.of(canonical.toLowerCase(Locale.ROOT)),
                        synonyms.entrySet().stream()
                                .filter(entry -> entry.getValue().equalsIgnoreCase(canonical))
                                .map(Map.Entry::getKey))
                .collect(Collectors.toSet());
    }
}
