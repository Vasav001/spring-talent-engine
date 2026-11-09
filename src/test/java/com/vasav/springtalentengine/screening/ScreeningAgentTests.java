package com.vasav.springtalentengine.screening;

import com.vasav.springtalentengine.ingestion.ResumeContent;
import com.vasav.springtalentengine.jobs.JobRubric;
import com.vasav.springtalentengine.jobs.JobStatus;
import com.vasav.springtalentengine.jobs.RequirementType;
import com.vasav.springtalentengine.screening.ScreeningAgent.AgentResult;
import com.vasav.springtalentengine.screening.ScreeningReport.CriterionResult;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ScreeningAgentTests {

    private static final String RESUME = """
            Backend Engineer at Acme, 2020 - 2025
            Built payment APIs with Java 21 and Spring Boot.
            Ran PostgreSQL migrations with Flyway.
            """;

    private static final String GOOD_CARD = """
            {"criteria": [
              {"skill": "Java", "score": 5, "evidence": "Built payment APIs with Java 21 and Spring Boot."},
              {"skill": "PostgreSQL", "score": 3, "evidence": "Ran PostgreSQL migrations with Flyway"},
              {"skill": "Kafka", "score": 0, "evidence": ""}
            ],
             "strengths": ["Payment APIs"], "gaps": ["No Kafka"], "recommendation": "YES",
             "summary": "Solid Java backend engineer."}
            """;

    private static final String INVENTED_KAFKA_CARD = """
            {"criteria": [
              {"skill": "Java", "score": 5, "evidence": "Built payment APIs with Java 21"},
              {"skill": "PostgreSQL", "score": 3, "evidence": "PostgreSQL migrations"},
              {"skill": "Kafka", "score": 4, "evidence": "Designed Kafka event pipelines"}
            ],
             "strengths": [], "gaps": [], "recommendation": "STRONG_YES", "summary": "Great fit."}
            """;

    private final JobRubric rubric = new JobRubric(UUID.randomUUID(), "Backend Engineer", JobStatus.PUBLISHED, List.of(
            new JobRubric.Item("Java", RequirementType.MUST, 5, 3),
            new JobRubric.Item("PostgreSQL", RequirementType.MUST, 3, null),
            new JobRubric.Item("Kafka", RequirementType.NICE, 2, null)));

    private final ResumeContent resume = new ResumeContent(UUID.randomUUID(), rubric.jobId(), "cv.pdf", RESUME, null);

    private final ScreeningAgent agent = new ScreeningAgent(new GithubTool(""));

    @Test
    void acceptsQuotedEvidenceInOneCall() {
        ScriptedChatModel model = new ScriptedChatModel(GOOD_CARD);

        AgentResult result = agent.screen(ChatClient.create(model), rubric, resume);

        assertThat(model.prompts()).hasSize(1);
        assertThat(result.verification().unsupportedSkills()).isEmpty();
        assertThat(result.verification().criteria()).extracting(CriterionResult::score).containsExactly(5, 3, 0);
        assertThat(ScoreCardVerifier.aiScore(rubric.items(), result.verification().criteria())).isEqualTo(68);
    }

    @Test
    void retriesOnceWithFeedbackWhenEvidenceIsNotInTheResume() {
        ScriptedChatModel model = new ScriptedChatModel(INVENTED_KAFKA_CARD, GOOD_CARD);

        AgentResult result = agent.screen(ChatClient.create(model), rubric, resume);

        assertThat(model.prompts()).hasSize(2);
        assertThat(model.prompts().get(1).getContents()).contains("does not appear in the resume for: Kafka");
        assertThat(result.verification().unsupportedSkills()).isEmpty();
        assertThat(result.card().recommendation()).isEqualTo(Recommendation.YES);
    }

    @Test
    void zeroesCriteriaThatStayUnsupportedAfterTheRetry() {
        ScriptedChatModel model = new ScriptedChatModel(INVENTED_KAFKA_CARD, INVENTED_KAFKA_CARD);

        AgentResult result = agent.screen(ChatClient.create(model), rubric, resume);

        CriterionResult kafka = result.verification().criteria().get(2);
        assertThat(kafka.score()).isZero();
        assertThat(kafka.verified()).isFalse();
        assertThat(result.verification().criteria().get(0).score()).isEqualTo(5);
    }

    @Test
    void failsWhenTheModelDoesNotReturnAScoreCard() {
        ScriptedChatModel model = new ScriptedChatModel("Sorry, I cannot help with that.");

        assertThatThrownBy(() -> agent.screen(ChatClient.create(model), rubric, resume))
                .isInstanceOf(RuntimeException.class);
    }
}
