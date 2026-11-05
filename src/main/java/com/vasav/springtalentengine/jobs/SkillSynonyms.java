package com.vasav.springtalentengine.jobs;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class SkillSynonyms {

    private final SkillSynonymRepository repository;

    public SkillDictionary load() {
        return new SkillDictionary(repository.findAll().stream()
                .collect(Collectors.toUnmodifiableMap(SkillSynonym::getAlias, SkillSynonym::getCanonical)));
    }
}
