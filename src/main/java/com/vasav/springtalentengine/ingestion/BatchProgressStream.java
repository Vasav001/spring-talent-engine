package com.vasav.springtalentengine.ingestion;

import com.vasav.springtalentengine.ingestion.BatchService.BatchProgress;
import com.vasav.springtalentengine.shared.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.Duration;
import java.util.UUID;

@Component
@RequiredArgsConstructor
class BatchProgressStream {

    private static final Duration TIMEOUT = Duration.ofMinutes(30);
    private static final Duration INTERVAL = Duration.ofSeconds(2);

    private final BatchService batchService;

    SseEmitter open(UUID batchId) {
        UUID tenantId = TenantContext.require();
        batchService.progress(batchId);
        SseEmitter emitter = new SseEmitter(TIMEOUT.toMillis());
        Thread.ofVirtual().start(() -> stream(tenantId, batchId, emitter));
        return emitter;
    }

    private void stream(UUID tenantId, UUID batchId, SseEmitter emitter) {
        TenantContext.set(tenantId);
        try {
            while (true) {
                BatchProgress progress = batchService.progress(batchId);
                emitter.send(SseEmitter.event().name("progress").data(progress));
                if (progress.status() == BatchStatus.COMPLETED) {
                    emitter.complete();
                    return;
                }
                Thread.sleep(INTERVAL);
            }
        } catch (IOException | IllegalStateException e) {
            return;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            emitter.complete();
        } catch (RuntimeException e) {
            emitter.completeWithError(e);
        } finally {
            TenantContext.clear();
        }
    }
}
