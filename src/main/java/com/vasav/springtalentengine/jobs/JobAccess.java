package com.vasav.springtalentengine.jobs;

import com.vasav.springtalentengine.shared.CurrentUser;
import com.vasav.springtalentengine.tenant.Permission;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Component("jobAccess")
@RequiredArgsConstructor
public class JobAccess {

    private final JobRepository jobRepository;

    @Transactional(readOnly = true)
    public boolean canEdit(UUID jobId) {
        if (!CurrentUser.hasAuthority(Permission.JOB_WRITE.name())) {
            return false;
        }
        if (CurrentUser.hasAuthority(Permission.JOB_MANAGE_ALL.name())) {
            return true;
        }
        return jobRepository.findById(jobId)
                .map(job -> job.getCreatedBy().equals(CurrentUser.requireEmail()))
                .orElse(true);
    }
}
