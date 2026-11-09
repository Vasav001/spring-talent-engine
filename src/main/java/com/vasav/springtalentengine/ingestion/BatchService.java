package com.vasav.springtalentengine.ingestion;

import com.vasav.springtalentengine.jobs.JobRubrics;
import com.vasav.springtalentengine.shared.CurrentUser;
import com.vasav.springtalentengine.shared.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
class BatchService {

    static final int MAX_FILES = 50;
    private static final Duration UPLOAD_URL_VALIDITY = Duration.ofMinutes(15);

    private final BatchRepository batchRepository;
    private final ResumeRepository resumeRepository;
    private final ResumeStorage storage;
    private final JobRubrics jobRubrics;
    private final ApplicationEventPublisher events;
    private final TransactionTemplate transactionTemplate;

    BatchCreated create(UUID jobId, List<String> fileNames) {
        UUID tenantId = TenantContext.require();
        ResumeService.requirePublishedJob(jobRubrics, jobId);
        List<String> names = fileNames.stream().map(ResumeFiles::cleanName).toList();
        names.stream()
                .filter(name -> ResumeFiles.contentType(name).isEmpty())
                .findFirst()
                .ifPresent(name -> {
                    throw new ResponseStatusException(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Only PDF and DOCX files are supported: " + name);
                });
        String email = CurrentUser.requireEmail();

        Batch batch = transactionTemplate.execute(status -> {
            Batch saved = batchRepository.save(new Batch(tenantId, jobId, names.size(), email));
            resumeRepository.saveAll(names.stream()
                    .map(name -> Resume.awaitingUpload(tenantId, jobId, saved.getId(), name,
                            ResumeFiles.contentType(name).orElseThrow(), ResumeFiles.storageKey(tenantId, jobId, name), email))
                    .toList());
            return saved;
        });

        List<UploadSlot> uploads = resumeRepository.findByBatchIdOrderByFileName(batch.getId()).stream()
                .map(resume -> new UploadSlot(resume.getId(), resume.getFileName(),
                        storage.uploadUrl(resume.getStorageKey(), UPLOAD_URL_VALIDITY).toString()))
                .toList();
        return new BatchCreated(batch.getId(), jobId, batch.getStatus(), batch.getTotal(),
                Instant.now().plus(UPLOAD_URL_VALIDITY), uploads);
    }

    BatchDetails submit(UUID batchId) {
        UUID tenantId = TenantContext.require();
        Set<UUID> uploaded = resumeRepository.findByBatchIdOrderByFileName(batchId).stream()
                .filter(resume -> resume.getStatus() == ResumeStatus.AWAITING_UPLOAD && storage.exists(resume.getStorageKey()))
                .map(Resume::getId)
                .collect(Collectors.toSet());

        return transactionTemplate.execute(status -> {
            Batch batch = batchRepository.findLockedById(batchId)
                    .filter(found -> found.getTenantId().equals(tenantId))
                    .orElseThrow(BatchService::notFound);
            if (batch.getStatus() != BatchStatus.AWAITING_UPLOAD) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Batch was already submitted");
            }
            List<Resume> resumes = resumeRepository.findByBatchIdOrderByFileName(batchId);
            resumes.forEach(resume -> {
                if (uploaded.contains(resume.getId())) {
                    resume.queue();
                } else {
                    resume.fail("File was not uploaded");
                }
            });
            List<UUID> queued = resumes.stream()
                    .filter(resume -> resume.getStatus() == ResumeStatus.QUEUED)
                    .map(Resume::getId)
                    .toList();
            batch.submit(resumes.size() - queued.size());
            if (!queued.isEmpty()) {
                events.publishEvent(new BatchSubmitted(tenantId, batchId, queued));
            }
            return BatchDetails.from(batch, resumes);
        });
    }

    @Transactional(readOnly = true)
    BatchDetails get(UUID batchId) {
        return BatchDetails.from(find(batchId), resumeRepository.findByBatchIdOrderByFileName(batchId));
    }

    @Transactional(readOnly = true)
    BatchProgress progress(UUID batchId) {
        return BatchProgress.from(find(batchId));
    }

    private Batch find(UUID batchId) {
        return batchRepository.findById(batchId)
                .filter(batch -> batch.getTenantId().equals(TenantContext.require()))
                .orElseThrow(BatchService::notFound);
    }

    private static ResponseStatusException notFound() {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, "Batch not found");
    }

    record UploadSlot(UUID resumeId, String fileName, String uploadUrl) {
    }

    record BatchCreated(UUID id, UUID jobId, BatchStatus status, int total, Instant uploadUrlsExpireAt,
                        List<UploadSlot> uploads) {
    }

    record BatchFile(UUID resumeId, String fileName, ResumeStatus status, String error) {
    }

    record BatchDetails(UUID id, UUID jobId, BatchStatus status, int total, int processed, int failed,
                        Instant createdAt, Instant completedAt, List<BatchFile> files) {

        static BatchDetails from(Batch batch, List<Resume> resumes) {
            return new BatchDetails(batch.getId(), batch.getJobId(), batch.getStatus(), batch.getTotal(),
                    batch.getProcessed(), batch.getFailed(), batch.getCreatedAt(), batch.getCompletedAt(),
                    resumes.stream()
                            .map(resume -> new BatchFile(resume.getId(), resume.getFileName(), resume.getStatus(), resume.getError()))
                            .toList());
        }
    }

    record BatchProgress(UUID batchId, BatchStatus status, int total, int processed, int failed) {

        static BatchProgress from(Batch batch) {
            return new BatchProgress(batch.getId(), batch.getStatus(), batch.getTotal(), batch.getProcessed(), batch.getFailed());
        }
    }
}
