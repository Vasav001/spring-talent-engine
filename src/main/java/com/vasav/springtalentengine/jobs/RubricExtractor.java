package com.vasav.springtalentengine.jobs;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Slf4j
@Component
class RubricExtractor {

    private static final String SYSTEM_PROMPT = """
            You extract a hiring rubric from a job description.
            Return every distinct skill, technology or qualification the candidate will be evaluated on.
            Rules:
            - skill: a short name such as "Java", "Spring Boot" or "Kubernetes", never a sentence.
            - type: MUST when the description says required, must have or minimum; NICE when it says preferred, nice to have, bonus or plus.
            - weight: 1 to 5, how important the item is for this role.
            - minYears: years of experience required for that item, only when the description states it, otherwise null.
            Return at most 15 items.
            """;

    private final ChatClient chatClient;

    RubricExtractor(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
    }

    List<RequirementDraft> extract(String jobDescription) {
        try {
            ExtractedRubric rubric = chatClient.prompt()
                    .system(SYSTEM_PROMPT)
                    .user(user -> user.text("Job description:\n{description}").param("description", jobDescription))
                    .call()
                    .entity(ExtractedRubric.class);
            return rubric == null || rubric.requirements() == null ? List.of() : rubric.requirements();
        } catch (RuntimeException e) {
            log.warn("Rubric extraction failed", e);
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Rubric extraction failed, try again or edit the rubric manually");
        }
    }

    record ExtractedRubric(List<RequirementDraft> requirements) {
    }
}
