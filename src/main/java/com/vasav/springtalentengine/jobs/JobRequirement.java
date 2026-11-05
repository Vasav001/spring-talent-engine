package com.vasav.springtalentengine.jobs;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@Table(name = "job_requirements")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
class JobRequirement {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private UUID tenantId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_id")
    private Job job;

    private String skill;

    @Enumerated(EnumType.STRING)
    private RequirementType type;

    private int weight;

    private Integer minYears;

    JobRequirement(Job job, RequirementDraft draft) {
        this.tenantId = job.getTenantId();
        this.job = job;
        this.skill = draft.skill();
        this.type = draft.type();
        this.weight = draft.weight();
        this.minYears = draft.minYears();
    }
}
