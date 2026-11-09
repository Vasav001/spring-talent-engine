package com.vasav.springtalentengine.billing;

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
@Table(name = "credit_ledger")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
class LedgerEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private UUID tenantId;

    @Enumerated(EnumType.STRING)
    private LedgerType type;

    private int amount;

    private UUID batchId;

    private UUID resumeId;

    private String note;

    private Instant createdAt;

    LedgerEntry(UUID tenantId, LedgerType type, int amount, UUID batchId, UUID resumeId, String note) {
        this.tenantId = tenantId;
        this.type = type;
        this.amount = amount;
        this.batchId = batchId;
        this.resumeId = resumeId;
        this.note = note;
        this.createdAt = Instant.now();
    }
}
