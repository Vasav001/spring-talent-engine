package com.vasav.springtalentengine.ingestion;

import com.vasav.springtalentengine.jobs.JobRubric;
import com.vasav.springtalentengine.jobs.JobRubrics;
import com.vasav.springtalentengine.jobs.JobStatus;
import com.vasav.springtalentengine.shared.CurrentUser;
import com.vasav.springtalentengine.shared.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
class ResumeService {

    private final ResumeRepository resumeRepository;
    private final ResumeParser parser;
    private final ResumeStorage storage;
    private final JobRubrics jobRubrics;

    ResumeDetails upload(UUID jobId, MultipartFile file) {
        UUID tenantId = TenantContext.require();
        requirePublishedJob(jobRubrics, jobId);

        String fileName = ResumeFiles.cleanName(file.getOriginalFilename());
        String contentType = ResumeFiles.contentType(fileName)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Only PDF and DOCX files are supported"));

        byte[] content = read(file);
        ParsedResume parsed;
        try {
            parsed = parser.parse(content);
        } catch (ResumeRejectedException e) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_CONTENT, e.getMessage());
        }
        if (resumeRepository.existsByJobIdAndContentHash(jobId, parsed.contentHash())) {
            throw duplicate();
        }

        String storageKey = ResumeFiles.storageKey(tenantId, jobId, fileName);
        storage.store(storageKey, content, contentType);
        Resume resume = Resume.uploaded(tenantId, jobId, fileName, contentType, storageKey, parsed, CurrentUser.requireEmail());
        try {
            return ResumeDetails.from(resumeRepository.save(resume));
        } catch (DataIntegrityViolationException e) {
            throw duplicate();
        }
    }

    static void requirePublishedJob(JobRubrics jobRubrics, UUID jobId) {
        JobRubric job = jobRubrics.find(jobId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Job not found"));
        if (job.status() != JobStatus.PUBLISHED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Resumes can only be added to published jobs");
        }
    }

    private static ResponseStatusException duplicate() {
        return new ResponseStatusException(HttpStatus.CONFLICT, "This resume was already uploaded for this job");
    }

    private static byte[] read(MultipartFile file) {
        try {
            return file.getBytes();
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Could not read the uploaded file");
        }
    }

    record ResumeDetails(UUID id, UUID jobId, String fileName, String githubUsername, int textLength, Instant createdAt) {

        static ResumeDetails from(Resume resume) {
            return new ResumeDetails(resume.getId(), resume.getJobId(), resume.getFileName(), resume.getGithubUsername(),
                    resume.getText().length(), resume.getCreatedAt());
        }
    }
}
