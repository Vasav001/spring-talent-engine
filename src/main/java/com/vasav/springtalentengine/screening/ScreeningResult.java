package com.vasav.springtalentengine.screening;

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
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "screening_results")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
class ScreeningResult {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private UUID tenantId;

    private UUID resumeId;

    private UUID jobId;

    private String fileName;

    @Enumerated(EnumType.STRING)
    private ScreeningStatus status;

    private int keywordScore;

    private Integer aiScore;

    private int finalScore;

    @Enumerated(EnumType.STRING)
    private Recommendation recommendation;

    @JdbcTypeCode(SqlTypes.JSON)
    private ScreeningReport report;

    private String error;

    private String model;

    private int tokensIn;

    private int tokensOut;

    private Instant screenedAt;

    ScreeningResult(UUID tenantId, UUID resumeId, UUID jobId, String fileName) {
        this.tenantId = tenantId;
        this.resumeId = resumeId;
        this.jobId = jobId;
        this.fileName = fileName;
    }

    void record(ScreeningOutcome outcome) {
        this.status = outcome.status();
        this.keywordScore = outcome.keywordScore();
        this.aiScore = outcome.aiScore();
        this.finalScore = outcome.finalScore();
        this.recommendation = outcome.recommendation();
        this.report = outcome.report();
        this.error = outcome.error();
        this.model = outcome.model();
        this.tokensIn = outcome.tokensIn();
        this.tokensOut = outcome.tokensOut();
        this.screenedAt = Instant.now();
    }
}
