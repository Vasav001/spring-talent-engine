package com.vasav.springtalentengine.ingestion;

import com.vasav.springtalentengine.shared.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ResumeContents {

    private final ResumeRepository resumeRepository;

    @Transactional(readOnly = true)
    public Optional<ResumeContent> find(UUID resumeId) {
        return resumeRepository.findById(resumeId)
                .filter(resume -> resume.getTenantId().equals(TenantContext.require()))
                .filter(resume -> resume.getText() != null)
                .map(ResumeContents::toContent);
    }

    static ResumeContent toContent(Resume resume) {
        return new ResumeContent(resume.getId(), resume.getJobId(), resume.getFileName(),
                resume.getText(), resume.getGithubUsername());
    }
}
