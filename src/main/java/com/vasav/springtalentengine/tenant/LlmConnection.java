package com.vasav.springtalentengine.tenant;

import java.time.Instant;

public record LlmConnection(String baseUrl, String model, String apiKey, Instant updatedAt) {

    @Override
    public String toString() {
        return "LlmConnection[baseUrl=%s, model=%s, apiKey=****]".formatted(baseUrl, model);
    }
}
