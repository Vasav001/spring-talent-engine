package com.vasav.springtalentengine.jobs;

import com.vasav.springtalentengine.shared.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class JobRubrics {

    private final JobRepository jobRepository;

    @Transactional(readOnly = true)
    public Optional<JobRubric> find(UUID jobId) {
        return jobRepository.findById(jobId)
                .filter(job -> job.getTenantId().equals(TenantContext.require()))
                .map(job -> new JobRubric(job.getId(), job.getTitle(), job.getStatus(), job.getRequirements().stream()
                        .map(requirement -> new JobRubric.Item(requirement.getSkill(), requirement.getType(),
                                requirement.getWeight(), requirement.getMinYears()))
                        .toList()));
    }
}
