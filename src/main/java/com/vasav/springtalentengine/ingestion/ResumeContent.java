package com.vasav.springtalentengine.ingestion;

import java.util.UUID;

public record ResumeContent(UUID resumeId, UUID jobId, String fileName, String text, String githubUsername) {
}
