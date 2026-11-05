package com.vasav.springtalentengine.jobs;

import com.vasav.springtalentengine.shared.CurrentUser;
import com.vasav.springtalentengine.shared.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
class JobService {

    private final JobRepository jobRepository;
    private final RubricExtractor rubricExtractor;
    private final SkillSynonyms skillSynonyms;
    private final TransactionTemplate transactionTemplate;

    JobDetails create(String title, String description) {
        UUID tenantId = TenantContext.require();
        String createdBy = CurrentUser.requireEmail();
        UUID jobId = transactionTemplate.execute(status ->
                jobRepository.save(new Job(tenantId, title, description, createdBy)).getId());
        List<RequirementDraft> extracted = extractOrEmpty(description);
        return transactionTemplate.execute(status -> replaceRubric(jobId, extracted));
    }

    @Transactional(readOnly = true)
    Page<JobSummary> list(JobStatus status, Pageable pageable) {
        UUID tenantId = TenantContext.require();
        Page<Job> jobs = status == null
                ? jobRepository.findByTenantId(tenantId, pageable)
                : jobRepository.findByTenantIdAndStatus(tenantId, status, pageable);
        return jobs.map(JobSummary::from);
    }

    @Transactional(readOnly = true)
    JobDetails get(UUID jobId) {
        return JobDetails.from(find(jobId));
    }

    @Transactional
    JobDetails updateRequirements(UUID jobId, List<RequirementDraft> drafts) {
        return replaceRubric(jobId, drafts);
    }

    JobDetails extractRubric(UUID jobId) {
        String description = transactionTemplate.execute(status -> findDraft(jobId).getDescription());
        List<RequirementDraft> extracted = rubricExtractor.extract(description);
        return transactionTemplate.execute(status -> replaceRubric(jobId, extracted));
    }

    @Transactional
    JobDetails publish(UUID jobId) {
        Job job = findDraft(jobId);
        if (job.getRequirements().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Add at least one requirement before publishing");
        }
        job.changeStatus(JobStatus.PUBLISHED);
        return JobDetails.from(job);
    }

    @Transactional
    JobDetails close(UUID jobId) {
        Job job = find(jobId);
        if (job.getStatus() != JobStatus.PUBLISHED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Only published jobs can be closed");
        }
        job.changeStatus(JobStatus.CLOSED);
        return JobDetails.from(job);
    }

    @Transactional
    void delete(UUID jobId) {
        jobRepository.delete(findDraft(jobId));
    }

    private List<RequirementDraft> extractOrEmpty(String description) {
        try {
            return rubricExtractor.extract(description);
        } catch (ResponseStatusException e) {
            return List.of();
        }
    }

    private JobDetails replaceRubric(UUID jobId, List<RequirementDraft> drafts) {
        Job job = findDraft(jobId);
        job.replaceRequirements(Rubric.clean(drafts, skillSynonyms.load()));
        return JobDetails.from(job);
    }

    private Job find(UUID jobId) {
        return jobRepository.findById(jobId)
                .filter(job -> job.getTenantId().equals(TenantContext.require()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Job not found"));
    }

    private Job findDraft(UUID jobId) {
        Job job = find(jobId);
        if (job.getStatus() != JobStatus.DRAFT) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Only draft jobs can be changed");
        }
        return job;
    }

    record JobSummary(UUID id, String title, JobStatus status, String createdBy, Instant createdAt) {

        static JobSummary from(Job job) {
            return new JobSummary(job.getId(), job.getTitle(), job.getStatus(), job.getCreatedBy(), job.getCreatedAt());
        }
    }

    record JobDetails(UUID id, String title, String description, JobStatus status, String createdBy,
                      Instant createdAt, Instant updatedAt, List<RequirementDetails> requirements) {

        static JobDetails from(Job job) {
            return new JobDetails(job.getId(), job.getTitle(), job.getDescription(), job.getStatus(), job.getCreatedBy(),
                    job.getCreatedAt(), job.getUpdatedAt(),
                    job.getRequirements().stream().map(RequirementDetails::from).toList());
        }
    }

    record RequirementDetails(String skill, RequirementType type, int weight, Integer minYears) {

        static RequirementDetails from(JobRequirement requirement) {
            return new RequirementDetails(requirement.getSkill(), requirement.getType(), requirement.getWeight(), requirement.getMinYears());
        }
    }
}
