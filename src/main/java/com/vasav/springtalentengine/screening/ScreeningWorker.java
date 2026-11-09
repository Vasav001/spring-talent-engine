package com.vasav.springtalentengine.screening;

import com.vasav.springtalentengine.ingestion.ResumeQueued;
import com.vasav.springtalentengine.shared.TenantContext;
import io.awspring.cloud.sqs.annotation.SqsListener;
import io.awspring.cloud.sqs.listener.SqsHeaders;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
class ScreeningWorker {

    static final int MAX_ATTEMPTS = 3;

    private final ScreeningService screeningService;

    @SqsListener(value = "${app.queue.screening}",
            maxConcurrentMessages = "${app.queue.concurrency}",
            maxMessagesPerPoll = "${app.queue.concurrency}")
    public void onMessage(ResumeQueued message,
                          @Header(SqsHeaders.MessageSystemAttributes.SQS_APPROXIMATE_RECEIVE_COUNT) int receiveCount) {
        TenantContext.set(message.tenantId());
        try {
            screeningService.screenQueued(message.resumeId(), receiveCount >= MAX_ATTEMPTS);
        } finally {
            TenantContext.clear();
        }
    }
}
