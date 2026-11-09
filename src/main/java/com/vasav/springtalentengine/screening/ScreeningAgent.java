package com.vasav.springtalentengine.screening;

import com.vasav.springtalentengine.ingestion.ResumeContent;
import com.vasav.springtalentengine.jobs.JobRubric;
import com.vasav.springtalentengine.screening.ScoreCardVerifier.Verification;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.ResponseEntity;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
class ScreeningAgent {

    private static final String SYSTEM_PROMPT = """
            You screen one resume against a hiring rubric for a recruiter.
            For every rubric item return: skill (exactly as written in the rubric), score from 0 to 5 and evidence.
            Scoring: 5 = strong, recent, hands-on experience that meets any minimum years; 3 = some real experience; 1 = only mentioned; 0 = no evidence.
            Evidence must be copied word for word from the resume, at most 30 words. When there is no evidence use an empty string and score 0.
            If the resume links a GitHub profile you may call getGithubSummary once. Use the result only for strengths and gaps, never as evidence.
            Judge only skills and experience. Ignore name, gender, age, nationality, photos and other personal details.
            Also return up to 5 strengths, up to 5 gaps, a recommendation (STRONG_YES, YES, MAYBE or NO) and a one sentence summary.
            """;

    private static final String USER_PROMPT = """
            Job title: {title}

            Rubric:
            {rubric}

            Resume:
            {resume}

            {feedback}
            """;

    private final GithubTool githubTool;

    ScreeningAgent(GithubTool githubTool) {
        this.githubTool = githubTool;
    }

    AgentResult screen(ChatClient chatClient, JobRubric rubric, ResumeContent resume) {
        Attempt first = ask(chatClient, rubric, resume, "");
        Verification verification = ScoreCardVerifier.verify(first.card(), rubric.items(), resume.text());
        if (verification.unsupportedSkills().isEmpty()) {
            return new AgentResult(first.card(), verification, first.model(), first.tokensIn(), first.tokensOut());
        }
        Attempt retry = ask(chatClient, rubric, resume, feedback(verification.unsupportedSkills()));
        return new AgentResult(retry.card(),
                ScoreCardVerifier.verify(retry.card(), rubric.items(), resume.text()),
                retry.model(),
                first.tokensIn() + retry.tokensIn(),
                first.tokensOut() + retry.tokensOut());
    }

    private Attempt ask(ChatClient chatClient, JobRubric rubric, ResumeContent resume, String feedback) {
        ResponseEntity<ChatResponse, AgentScoreCard> response = chatClient.prompt()
                .system(SYSTEM_PROMPT)
                .user(user -> user.text(USER_PROMPT)
                        .param("title", rubric.title())
                        .param("rubric", formatRubric(rubric))
                        .param("resume", resume.text())
                        .param("feedback", feedback))
                .tools(githubTool)
                .call()
                .responseEntity(AgentScoreCard.class);
        AgentScoreCard card = response.getEntity();
        if (card == null) {
            throw new IllegalStateException("Model returned no score card");
        }
        ChatResponse chatResponse = response.getResponse();
        Usage usage = chatResponse == null ? null : chatResponse.getMetadata().getUsage();
        return new Attempt(card,
                chatResponse == null ? null : chatResponse.getMetadata().getModel(),
                usage == null || usage.getPromptTokens() == null ? 0 : usage.getPromptTokens(),
                usage == null || usage.getCompletionTokens() == null ? 0 : usage.getCompletionTokens());
    }

    private static String formatRubric(JobRubric rubric) {
        return rubric.items().stream()
                .map(item -> "- %s | %s | weight %d%s".formatted(item.skill(), item.type(), item.weight(),
                        item.minYears() == null ? "" : " | at least %d years".formatted(item.minYears())))
                .collect(Collectors.joining("\n"));
    }

    private static String feedback(List<String> unsupportedSkills) {
        return "Your previous answer used evidence that does not appear in the resume for: %s. Copy evidence exactly from the resume, or use an empty string and score 0."
                .formatted(String.join(", ", unsupportedSkills));
    }

    private record Attempt(AgentScoreCard card, String model, int tokensIn, int tokensOut) {
    }

    record AgentResult(AgentScoreCard card, Verification verification, String model, int tokensIn, int tokensOut) {
    }
}
