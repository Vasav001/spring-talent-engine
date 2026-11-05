package com.vasav.springtalentengine.jobs;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "jobs")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
class Job {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private UUID tenantId;

    private String title;

    private String description;

    @Enumerated(EnumType.STRING)
    private JobStatus status;

    private String createdBy;

    private Instant createdAt;

    private Instant updatedAt;

    @OneToMany(mappedBy = "job", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("weight DESC")
    private List<JobRequirement> requirements = new ArrayList<>();

    Job(UUID tenantId, String title, String description, String createdBy) {
        this.tenantId = tenantId;
        this.title = title;
        this.description = description;
        this.status = JobStatus.DRAFT;
        this.createdBy = createdBy;
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    void replaceRequirements(List<RequirementDraft> drafts) {
        requirements.clear();
        drafts.forEach(draft -> requirements.add(new JobRequirement(this, draft)));
        updatedAt = Instant.now();
    }

    void changeStatus(JobStatus status) {
        this.status = status;
        updatedAt = Instant.now();
    }
}
