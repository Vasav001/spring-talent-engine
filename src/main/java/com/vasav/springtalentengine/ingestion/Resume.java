package com.vasav.springtalentengine.ingestion;

import jakarta.persistence.Entity;
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

    private String fileName;

    private String contentType;

    private String storageKey;

    private String contentHash;

    private String text;

    private String githubUsername;

    private String uploadedBy;

    private Instant createdAt;

    Resume(UUID tenantId, UUID jobId, StoredFile file, String text, String githubUsername, String uploadedBy) {
        this.tenantId = tenantId;
        this.jobId = jobId;
        this.fileName = file.fileName();
        this.contentType = file.contentType();
        this.storageKey = file.storageKey();
        this.contentHash = file.contentHash();
        this.text = text;
        this.githubUsername = githubUsername;
        this.uploadedBy = uploadedBy;
        this.createdAt = Instant.now();
    }

    record StoredFile(String fileName, String contentType, String storageKey, String contentHash) {
    }
}
