package com.vasav.springtalentengine.ingestion;

import java.util.List;
import java.util.UUID;

record BatchSubmitted(UUID tenantId, UUID batchId, List<UUID> resumeIds) {
}
