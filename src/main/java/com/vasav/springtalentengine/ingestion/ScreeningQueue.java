package com.vasav.springtalentengine.ingestion;

import io.awspring.cloud.sqs.operations.SqsTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

@Component
class ScreeningQueue {

    private final SqsTemplate sqsTemplate;
    private final String queue;

    ScreeningQueue(SqsTemplate sqsTemplate, @Value("${app.queue.screening}") String queue) {
        this.sqsTemplate = sqsTemplate;
        this.queue = queue;
    }

    @ApplicationModuleListener
    public void on(BatchSubmitted event) {
        event.resumeIds().forEach(resumeId -> sqsTemplate.send(queue, new ResumeQueued(event.tenantId(), resumeId)));
    }
}
