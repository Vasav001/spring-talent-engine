package com.vasav.springtalentengine.tenant;

import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LlmSettingsTests {

    @Test
    void acceptsPublicHttpsUrls() {
        assertThat(LlmSettingsService.requirePublicHttpsUrl("https://api.groq.com/openai/"))
                .isEqualTo("https://api.groq.com/openai");
    }

    @Test
    void rejectsUrlsThatCouldReachInternalServices() {
        for (String url : new String[]{"http://api.groq.com", "https://localhost:8080", "https://169.254.169.254/latest",
                "https://[::1]/", "https://10.0.0.5", "not a url"}) {
            assertThatThrownBy(() -> LlmSettingsService.requirePublicHttpsUrl(url))
                    .as(url)
                    .isInstanceOf(ResponseStatusException.class);
        }
    }

    @Test
    void encryptsAndDecryptsApiKeys() {
        ApiKeyCipher cipher = new ApiKeyCipher("test-master-key", "8f3c2a7d5e1b9046");

        String encrypted = cipher.encrypt("gsk_secret_value");

        assertThat(encrypted).doesNotContain("gsk_secret_value");
        assertThat(cipher.decrypt(encrypted)).isEqualTo("gsk_secret_value");
    }

    @Test
    void neverPrintsTheApiKey() {
        LlmConnection connection = new LlmConnection("https://api.groq.com/openai", "model", "gsk_secret_value", Instant.now());

        assertThat(connection.toString()).doesNotContain("gsk_secret_value");
    }
}
