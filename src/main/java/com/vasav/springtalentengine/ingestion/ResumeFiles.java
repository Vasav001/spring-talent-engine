package com.vasav.springtalentengine.ingestion;

import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

final class ResumeFiles {

    static final long MAX_SIZE_BYTES = 5L * 1024 * 1024;

    private static final Map<String, String> CONTENT_TYPES = Map.of(
            "pdf", "application/pdf",
            "docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document");

    private ResumeFiles() {
    }

    static String cleanName(String fileName) {
        return fileName == null || fileName.isBlank() ? "resume" : fileName.strip();
    }

    static Optional<String> contentType(String fileName) {
        return Optional.ofNullable(CONTENT_TYPES.get(extension(fileName)));
    }

    static String storageKey(UUID tenantId, UUID jobId, String fileName) {
        return "tenants/%s/jobs/%s/%s.%s".formatted(tenantId, jobId, UUID.randomUUID(), extension(fileName));
    }

    private static String extension(String fileName) {
        int dot = fileName.lastIndexOf('.');
        return dot < 0 ? "" : fileName.substring(dot + 1).toLowerCase(Locale.ROOT);
    }
}
