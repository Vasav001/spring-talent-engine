package com.vasav.springtalentengine.tenant;

import com.vasav.springtalentengine.tenant.LlmSettingsService.LlmSettingsDetails;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/tenants/llm-settings")
@RequiredArgsConstructor
class LlmSettingsController {

    private final LlmSettingsService settingsService;

    @GetMapping
    @PreAuthorize("hasAuthority('BYOK_MANAGE')")
    public ResponseEntity<LlmSettingsDetails> get() {
        return ResponseEntity.of(settingsService.get());
    }

    @PutMapping
    @PreAuthorize("hasAuthority('BYOK_MANAGE')")
    public LlmSettingsDetails save(@Valid @RequestBody LlmSettingsRequest request) {
        return settingsService.save(request.baseUrl(), request.model(), request.apiKey());
    }

    @DeleteMapping
    @PreAuthorize("hasAuthority('BYOK_MANAGE')")
    public ResponseEntity<Void> delete() {
        settingsService.delete();
        return ResponseEntity.noContent().build();
    }

    record LlmSettingsRequest(@NotBlank @Size(max = 200) String baseUrl,
                              @NotBlank @Size(max = 100) String model,
                              @NotBlank @Size(min = 8, max = 500) String apiKey) {

        @Override
        public String toString() {
            return "LlmSettingsRequest[baseUrl=%s, model=%s, apiKey=****]".formatted(baseUrl, model);
        }
    }
}
