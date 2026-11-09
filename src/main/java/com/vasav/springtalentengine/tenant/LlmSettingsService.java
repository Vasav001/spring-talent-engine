package com.vasav.springtalentengine.tenant;

import com.vasav.springtalentengine.shared.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;
import java.time.Instant;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
class LlmSettingsService {

    private static final Pattern IPV4 = Pattern.compile("\\d{1,3}(\\.\\d{1,3}){3}");

    private final TenantLlmSettingsRepository settingsRepository;
    private final ApiKeyCipher cipher;

    @Transactional(readOnly = true)
    Optional<LlmSettingsDetails> get() {
        return settingsRepository.findById(TenantContext.require()).map(LlmSettingsDetails::from);
    }

    @Transactional
    LlmSettingsDetails save(String baseUrl, String model, String apiKey) {
        UUID tenantId = TenantContext.require();
        String cleanBaseUrl = requirePublicHttpsUrl(baseUrl.strip());
        String cleanKey = apiKey.strip();
        TenantLlmSettings settings = settingsRepository.findById(tenantId).orElseGet(() -> new TenantLlmSettings(tenantId));
        settings.update(cleanBaseUrl, model.strip(), cipher.encrypt(cleanKey), cleanKey.substring(cleanKey.length() - 4));
        return LlmSettingsDetails.from(settingsRepository.save(settings));
    }

    @Transactional
    void delete() {
        settingsRepository.deleteById(TenantContext.require());
    }

    static String requirePublicHttpsUrl(String value) {
        URI uri;
        try {
            uri = URI.create(value);
        } catch (IllegalArgumentException e) {
            throw invalidUrl();
        }
        String host = uri.getHost() == null ? "" : uri.getHost().toLowerCase(Locale.ROOT);
        if (!"https".equalsIgnoreCase(uri.getScheme()) || host.isEmpty() || host.equals("localhost")
                || host.endsWith(".localhost") || host.contains(":") || host.startsWith("[") || IPV4.matcher(host).matches()) {
            throw invalidUrl();
        }
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }

    private static ResponseStatusException invalidUrl() {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, "Base URL must be an https URL with a public host name");
    }

    record LlmSettingsDetails(String baseUrl, String model, String apiKey, Instant updatedAt) {

        static LlmSettingsDetails from(TenantLlmSettings settings) {
            return new LlmSettingsDetails(settings.getBaseUrl(), settings.getModel(), "****" + settings.getApiKeyLast4(),
                    settings.getUpdatedAt());
        }
    }
}
