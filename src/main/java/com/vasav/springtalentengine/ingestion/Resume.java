package com.vasav.springtalentengine.ingestion;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "resumes")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
class Resume {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private UUID tenantId;

    private UUID jobId;

    private UUID batchId;

    private String fileName;

    private String contentType;

    private String storageKey;

    private String contentHash;

    private String text;

    private String githubUsername;

    @Enumerated(EnumType.STRING)
    private ResumeStatus status;

    private String error;

    private String uploadedBy;

    private Instant createdAt;

    private Resume(UUID tenantId, UUID jobId, UUID batchId, String fileName, String contentType, String storageKey,
                   ResumeStatus status, String uploadedBy) {
        this.tenantId = tenantId;
        this.jobId = jobId;
        this.batchId = batchId;
        this.fileName = fileName;
        this.contentType = contentType;
        this.storageKey = storageKey;
        this.status = status;
        this.uploadedBy = uploadedBy;
        this.createdAt = Instant.now();
    }

    static Resume uploaded(UUID tenantId, UUID jobId, String fileName, String contentType, String storageKey,
                           ParsedResume parsed, String uploadedBy) {
        Resume resume = new Resume(tenantId, jobId, null, fileName, contentType, storageKey, ResumeStatus.DONE, uploadedBy);
        resume.attach(parsed);
        return resume;
    }

    static Resume awaitingUpload(UUID tenantId, UUID jobId, UUID batchId, String fileName, String contentType,
                                 String storageKey, String uploadedBy) {
        return new Resume(tenantId, jobId, batchId, fileName, contentType, storageKey, ResumeStatus.AWAITING_UPLOAD, uploadedBy);
    }

    void attach(ParsedResume parsed) {
        this.contentHash = parsed.contentHash();
        this.text = parsed.text();
        this.githubUsername = parsed.githubUsername();
    }

    void queue() {
        this.status = ResumeStatus.QUEUED;
    }

    void fail(String error) {
        this.status = ResumeStatus.FAILED;
        this.error = error;
    }
}
