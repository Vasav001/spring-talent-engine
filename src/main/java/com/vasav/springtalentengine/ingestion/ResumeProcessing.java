package com.vasav.springtalentengine.ingestion;

import com.vasav.springtalentengine.shared.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ResumeProcessing {

    private final ResumeRepository resumeRepository;
    private final BatchRepository batchRepository;
    private final ResumeStorage storage;
    private final ResumeParser parser;

    public Optional<ResumeContent> prepare(UUID resumeId) {
        Optional<Resume> queued = resumeRepository.findById(resumeId)
                .filter(resume -> resume.getTenantId().equals(TenantContext.require()))
                .filter(resume -> resume.getStatus() == ResumeStatus.QUEUED);
        if (queued.isEmpty()) {
            return Optional.empty();
        }
        Resume resume = queued.get();
        if (resume.getText() != null) {
            return Optional.of(ResumeContents.toContent(resume));
        }

        ParsedResume parsed = parser.parse(storage.read(resume.getStorageKey()));
        if (resumeRepository.existsByJobIdAndContentHash(resume.getJobId(), parsed.contentHash())) {
            throw duplicate();
        }
        resume.attach(parsed);
        try {
            return Optional.of(ResumeContents.toContent(resumeRepository.saveAndFlush(resume)));
        } catch (DataIntegrityViolationException e) {
            throw duplicate();
        }
    }

    @Transactional
    public void finish(UUID resumeId, String error) {
        resumeRepository.findById(resumeId).ifPresent(resume -> {
            ResumeStatus status = error == null ? ResumeStatus.DONE : ResumeStatus.FAILED;
            boolean firstFinish = resumeRepository.finishQueued(resumeId, status, error) == 1;
            if (firstFinish && resume.getBatchId() != null) {
                batchRepository.recordProgress(resume.getBatchId(), error == null ? 0 : 1);
            }
        });
    }

    private static ResumeRejectedException duplicate() {
        return new ResumeRejectedException("This resume was already uploaded for this job");
    }
}
