package com.vasav.springtalentengine.jobs;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class RubricTests {

    private final SkillDictionary dictionary = new SkillDictionary(Map.of(
            "k8s", "Kubernetes",
            "kubernetes", "Kubernetes",
            "springboot", "Spring Boot"));

    @Test
    void normalizesSkillNamesThroughSynonyms() {
        List<RequirementDraft> cleaned = Rubric.clean(List.of(
                new RequirementDraft(" k8s ", RequirementType.MUST, 4, null),
                new RequirementDraft("SpringBoot", RequirementType.NICE, 2, null),
                new RequirementDraft("Rust", RequirementType.NICE, 1, null)), dictionary);

        assertThat(cleaned).extracting(RequirementDraft::skill).containsExactly("Kubernetes", "Spring Boot", "Rust");
    }

    @Test
    void mergesDuplicatesKeepingTheStrongerRequirement() {
        List<RequirementDraft> cleaned = Rubric.clean(List.of(
                new RequirementDraft("kubernetes", RequirementType.NICE, 5, null),
                new RequirementDraft("k8s", RequirementType.MUST, 2, 3)), dictionary);

        assertThat(cleaned).containsExactly(new RequirementDraft("Kubernetes", RequirementType.MUST, 2, 3));
    }

    @Test
    void clampsValuesAndDropsInvalidItems() {
        List<RequirementDraft> cleaned = Rubric.clean(List.of(
                new RequirementDraft("Java", RequirementType.MUST, 9, 99),
                new RequirementDraft("Go", RequirementType.NICE, null, -2),
                new RequirementDraft(" ", RequirementType.MUST, 3, null),
                new RequirementDraft("Docker", null, 3, null)), dictionary);

        assertThat(cleaned).containsExactly(
                new RequirementDraft("Java", RequirementType.MUST, 5, 40),
                new RequirementDraft("Go", RequirementType.NICE, 3, 0));
    }

    @Test
    void limitsTheNumberOfRequirements() {
        List<RequirementDraft> drafts = new ArrayList<>();
        for (int i = 0; i < 30; i++) {
            drafts.add(new RequirementDraft("Skill " + i, RequirementType.NICE, 3, null));
        }

        assertThat(Rubric.clean(drafts, dictionary)).hasSize(Rubric.MAX_REQUIREMENTS);
    }
}
