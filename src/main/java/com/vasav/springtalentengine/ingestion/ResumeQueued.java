package com.vasav.springtalentengine.ingestion;

import java.util.UUID;

public record ResumeQueued(UUID tenantId, UUID resumeId) {
}
