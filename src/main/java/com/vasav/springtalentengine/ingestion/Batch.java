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
@Table(name = "batches")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
class Batch {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private UUID tenantId;

    private UUID jobId;

    @Enumerated(EnumType.STRING)
    private BatchStatus status;

    private int total;

    private int processed;

    private int failed;

    private String createdBy;

    private Instant createdAt;

    private Instant completedAt;

    Batch(UUID tenantId, UUID jobId, int total, String createdBy) {
        this.tenantId = tenantId;
        this.jobId = jobId;
        this.total = total;
        this.status = BatchStatus.AWAITING_UPLOAD;
        this.createdBy = createdBy;
        this.createdAt = Instant.now();
    }

    void submit(int missingFiles) {
        this.status = BatchStatus.PROCESSING;
        this.processed = missingFiles;
        this.failed = missingFiles;
        if (processed >= total) {
            this.status = BatchStatus.COMPLETED;
            this.completedAt = Instant.now();
        }
    }
}
