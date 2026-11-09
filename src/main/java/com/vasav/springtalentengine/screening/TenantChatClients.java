package com.vasav.springtalentengine.screening;

import com.vasav.springtalentengine.billing.Credits;
import com.vasav.springtalentengine.shared.TenantContext;
import com.vasav.springtalentengine.tenant.LlmConnection;
import com.vasav.springtalentengine.tenant.LlmConnections;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
class TenantChatClients {

    static final String MISSING_SETTINGS = "Your plan uses your own LLM key, add it under /tenants/llm-settings";

    private final ChatClient platformClient;
    private final Credits credits;
    private final LlmConnections llmConnections;
    private final Map<UUID, CachedClient> cache = new ConcurrentHashMap<>();

    TenantChatClients(ChatClient.Builder chatClientBuilder, Credits credits, LlmConnections llmConnections) {
        this.platformClient = chatClientBuilder.build();
        this.credits = credits;
        this.llmConnections = llmConnections;
    }

    Optional<ChatClient> current() {
        if (!credits.usesOwnKey()) {
            return Optional.of(platformClient);
        }
        return llmConnections.find().map(this::clientFor);
    }

    private ChatClient clientFor(LlmConnection connection) {
        UUID tenantId = TenantContext.require();
        CachedClient cached = cache.get(tenantId);
        if (cached != null && cached.updatedAt().equals(connection.updatedAt())) {
            return cached.client();
        }
        ChatClient client = ChatClient.create(OpenAiChatModel.builder()
                .options(OpenAiChatOptions.builder()
                        .baseUrl(connection.baseUrl())
                        .apiKey(connection.apiKey())
                        .model(connection.model())
                        .temperature(0.0)
                        .build())
                .build());
        cache.put(tenantId, new CachedClient(connection.updatedAt(), client));
        return client;
    }

    private record CachedClient(Instant updatedAt, ChatClient client) {
    }
}
