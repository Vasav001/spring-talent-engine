package com.vasav.springtalentengine.jobs;

import com.vasav.springtalentengine.jobs.JobService.JobDetails;
import com.vasav.springtalentengine.jobs.JobService.JobSummary;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/jobs")
@RequiredArgsConstructor
class JobController {

    private final JobService jobService;

    @PostMapping
    @PreAuthorize("hasAuthority('JOB_WRITE')")
    public ResponseEntity<JobDetails> create(@Valid @RequestBody CreateJobRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(jobService.create(request.title(), request.description()));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('JOB_READ')")
    public Page<JobSummary> list(@RequestParam(required = false) JobStatus status,
                                 @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return jobService.list(status, pageable);
    }

    @GetMapping("/{jobId}")
    @PreAuthorize("hasAuthority('JOB_READ')")
    public JobDetails get(@PathVariable UUID jobId) {
        return jobService.get(jobId);
    }

    @PutMapping("/{jobId}/requirements")
    @PreAuthorize("@jobAccess.canEdit(#jobId)")
    public JobDetails updateRequirements(@PathVariable UUID jobId, @Valid @RequestBody UpdateRequirementsRequest request) {
        return jobService.updateRequirements(jobId, request.requirements().stream().map(RequirementRequest::toDraft).toList());
    }

    @PostMapping("/{jobId}/extract-rubric")
    @PreAuthorize("@jobAccess.canEdit(#jobId)")
    public JobDetails extractRubric(@PathVariable UUID jobId) {
        return jobService.extractRubric(jobId);
    }

    @PostMapping("/{jobId}/publish")
    @PreAuthorize("@jobAccess.canEdit(#jobId)")
    public JobDetails publish(@PathVariable UUID jobId) {
        return jobService.publish(jobId);
    }

    @PostMapping("/{jobId}/close")
    @PreAuthorize("@jobAccess.canEdit(#jobId)")
    public JobDetails close(@PathVariable UUID jobId) {
        return jobService.close(jobId);
    }

    @DeleteMapping("/{jobId}")
    @PreAuthorize("@jobAccess.canEdit(#jobId)")
    public ResponseEntity<Void> delete(@PathVariable UUID jobId) {
        jobService.delete(jobId);
        return ResponseEntity.noContent().build();
    }

    record CreateJobRequest(@NotBlank @Size(max = 150) String title, @NotBlank @Size(max = 20000) String description) {
    }

    record UpdateRequirementsRequest(@NotNull @Size(max = Rubric.MAX_REQUIREMENTS) List<@Valid @NotNull RequirementRequest> requirements) {
    }

    record RequirementRequest(@NotBlank @Size(max = 60) String skill,
                              @NotNull RequirementType type,
                              @NotNull @Min(1) @Max(5) Integer weight,
                              @Min(0) @Max(40) Integer minYears) {

        RequirementDraft toDraft() {
            return new RequirementDraft(skill, type, weight, minYears);
        }
    }
}
