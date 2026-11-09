package com.vasav.springtalentengine.tenant;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "tenant_llm_settings")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
class TenantLlmSettings {

    @Id
    private UUID tenantId;

    private String baseUrl;

    private String model;

    private String apiKeyEncrypted;

    private String apiKeyLast4;

    private Instant updatedAt;

    TenantLlmSettings(UUID tenantId) {
        this.tenantId = tenantId;
    }

    void update(String baseUrl, String model, String apiKeyEncrypted, String apiKeyLast4) {
        this.baseUrl = baseUrl;
        this.model = model;
        this.apiKeyEncrypted = apiKeyEncrypted;
        this.apiKeyLast4 = apiKeyLast4;
        this.updatedAt = Instant.now();
    }
}
