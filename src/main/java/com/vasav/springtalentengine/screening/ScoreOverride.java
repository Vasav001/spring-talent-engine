package com.vasav.springtalentengine.screening;

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
@Table(name = "score_overrides")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
class ScoreOverride {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private UUID tenantId;

    private UUID resultId;

    private int previousScore;

    private Integer newScore;

    private String reason;

    private String reviewerEmail;

    private Instant createdAt;

    ScoreOverride(ScreeningResult result, int previousScore, String reviewerEmail) {
        this.tenantId = result.getTenantId();
        this.resultId = result.getId();
        this.previousScore = previousScore;
        this.newScore = result.getOverrideScore();
        this.reason = result.getOverrideReason();
        this.reviewerEmail = reviewerEmail;
        this.createdAt = Instant.now();
    }
}
