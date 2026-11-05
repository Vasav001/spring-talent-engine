package com.vasav.springtalentengine.screening;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.net.http.HttpClient;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Component
class GithubTool {

    private static final Pattern USERNAME = Pattern.compile("[A-Za-z0-9][A-Za-z0-9-]{0,38}");
    private static final Duration TIMEOUT = Duration.ofSeconds(5);

    private final RestClient restClient;

    GithubTool(@Value("${app.github.token:}") String token) {
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(
                HttpClient.newBuilder().connectTimeout(TIMEOUT).build());
        requestFactory.setReadTimeout(TIMEOUT);
        RestClient.Builder builder = RestClient.builder()
                .baseUrl("https://api.github.com")
                .requestFactory(requestFactory)
                .defaultHeader("Accept", "application/vnd.github+json");
        if (!token.isBlank()) {
            builder.defaultHeader("Authorization", "Bearer " + token);
        }
        this.restClient = builder.build();
    }

    @Tool(description = "Summarize a candidate's public GitHub activity: number of own repositories, top languages and most recently updated repositories.")
    String getGithubSummary(@ToolParam(description = "GitHub username taken from the profile link in the resume") String username) {
        if (username == null || !USERNAME.matcher(username).matches()) {
            return "Invalid GitHub username.";
        }
        try {
            List<Repository> repositories = restClient.get()
                    .uri("/users/{username}/repos?per_page=100&sort=pushed", username)
                    .retrieve()
                    .body(new ParameterizedTypeReference<List<Repository>>() {
                    });
            return summarize(repositories == null ? List.of() : repositories);
        } catch (RestClientException e) {
            log.info("GitHub lookup failed for {}: {}", username, e.getMessage());
            return "GitHub profile is not available.";
        }
    }

    private static String summarize(List<Repository> repositories) {
        List<Repository> own = repositories.stream().filter(repository -> !repository.fork()).toList();
        if (own.isEmpty()) {
            return "No public repositories of their own.";
        }
        String languages = own.stream()
                .map(Repository::language)
                .filter(Objects::nonNull)
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()))
                .entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(5)
                .map(entry -> entry.getKey() + " (" + entry.getValue() + ")")
                .collect(Collectors.joining(", "));
        String recent = own.stream()
                .filter(repository -> repository.pushedAt() != null)
                .sorted(Comparator.comparing(Repository::pushedAt).reversed())
                .limit(5)
                .map(repository -> "%s (%s, last push %s, %d stars)".formatted(
                        repository.name(), repository.language(), repository.pushedAt().toString().substring(0, 10), repository.stars()))
                .collect(Collectors.joining("; "));
        return "%d own public repositories. Top languages: %s. Recently updated: %s.".formatted(own.size(), languages, recent);
    }

    record Repository(String name,
                      String language,
                      boolean fork,
                      @JsonProperty("stargazers_count") int stars,
                      @JsonProperty("pushed_at") Instant pushedAt) {
    }
}
