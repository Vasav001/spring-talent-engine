package com.vasav.springtalentengine.screening;

import com.vasav.springtalentengine.ingestion.ResumeContent;
import com.vasav.springtalentengine.jobs.JobRubric;
import com.vasav.springtalentengine.jobs.JobStatus;
import com.vasav.springtalentengine.jobs.RequirementType;
import com.vasav.springtalentengine.jobs.SkillDictionary;
import com.vasav.springtalentengine.screening.KeywordGate.KeywordMatch;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

@EnabledIfEnvironmentVariable(named = "LLM_API_KEY", matches = ".+")
class ScreeningEvalTests {

    private static final double MIN_AGREEMENT = 0.8;

    private final JobRubric rubric = new JobRubric(UUID.randomUUID(), "Senior Java Backend Engineer", JobStatus.PUBLISHED, List.of(
            new JobRubric.Item("Java", RequirementType.MUST, 5, 4),
            new JobRubric.Item("Spring Boot", RequirementType.MUST, 4, null),
            new JobRubric.Item("PostgreSQL", RequirementType.MUST, 3, null),
            new JobRubric.Item("Kubernetes", RequirementType.NICE, 2, null),
            new JobRubric.Item("Kafka", RequirementType.NICE, 2, null),
            new JobRubric.Item("AWS", RequirementType.NICE, 1, null)));

    private final SkillDictionary dictionary = new SkillDictionary(Map.of(
            "k8s", "Kubernetes",
            "kubernetes", "Kubernetes",
            "postgres", "PostgreSQL",
            "postgresql", "PostgreSQL"));

    @Test
    void rankingAgreesWithTheHandRanking() throws IOException, URISyntaxException {
        ChatClient chatClient = ChatClient.create(OpenAiChatModel.builder()
                .options(OpenAiChatOptions.builder()
                        .baseUrl(System.getenv("LLM_BASE_URL"))
                        .apiKey(System.getenv("LLM_API_KEY"))
                        .model(System.getenv("LLM_MODEL"))
                        .temperature(0.0)
                        .build())
                .build());
        ScreeningAgent agent = new ScreeningAgent(new GithubTool(""));

        List<Path> resumes = handRankedResumes();
        Map<String, Integer> scores = new LinkedHashMap<>();
        for (Path file : resumes) {
            String text = Files.readString(file);
            ResumeContent resume = new ResumeContent(UUID.randomUUID(), rubric.jobId(), file.getFileName().toString(), text, null);
            KeywordMatch keywords = KeywordGate.evaluate(rubric.items(), text, dictionary);
            ScreeningOutcome outcome = keywords.passes()
                    ? ScreeningOutcome.screened(keywords, rubric.items(), agent.screen(chatClient, rubric, resume))
                    : ScreeningOutcome.lowMatch(keywords);
            scores.put(resume.fileName(), outcome.finalScore());
            System.out.printf("%-35s %-10s keyword=%3d ai=%4s final=%3d%n", resume.fileName(), outcome.status(),
                    outcome.keywordScore(), outcome.aiScore(), outcome.finalScore());
        }

        double agreement = RankingAgreement.pairwise(resumes.stream().map(file -> file.getFileName().toString()).toList(), scores);
        System.out.printf("Pairwise agreement with the hand ranking: %.2f%n", agreement);
        assertThat(agreement).isGreaterThanOrEqualTo(MIN_AGREEMENT);
    }

    private List<Path> handRankedResumes() throws IOException, URISyntaxException {
        Path folder = Path.of(getClass().getResource("/eval/backend-java").toURI());
        try (Stream<Path> files = Files.list(folder)) {
            return files.filter(file -> file.toString().endsWith(".txt")).sorted().toList();
        }
    }
}
