package com.vasav.springtalentengine.tenant;

import com.vasav.springtalentengine.shared.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class LlmConnections {

    private final TenantLlmSettingsRepository settingsRepository;
    private final ApiKeyCipher cipher;

    @Transactional(readOnly = true)
    public Optional<LlmConnection> find() {
        return settingsRepository.findById(TenantContext.require())
                .map(settings -> new LlmConnection(settings.getBaseUrl(), settings.getModel(),
                        cipher.decrypt(settings.getApiKeyEncrypted()), settings.getUpdatedAt()));
    }
}
