package com.vasav.springtalentengine.ingestion;

import com.vasav.springtalentengine.jobs.JobRubric;
import com.vasav.springtalentengine.jobs.JobRubrics;
import com.vasav.springtalentengine.jobs.JobStatus;
import com.vasav.springtalentengine.shared.CurrentUser;
import com.vasav.springtalentengine.shared.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
class ResumeService {

    private static final Map<String, String> CONTENT_TYPES = Map.of(
            "pdf", "application/pdf",
            "docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document");

    private final ResumeRepository resumeRepository;
    private final ResumeTextExtractor textExtractor;
    private final ResumeStorage storage;
    private final JobRubrics jobRubrics;

    ResumeDetails upload(UUID jobId, MultipartFile file) {
        UUID tenantId = TenantContext.require();
        JobRubric job = jobRubrics.find(jobId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Job not found"));
        if (job.status() != JobStatus.PUBLISHED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Resumes can only be added to published jobs");
        }

        String fileName = file.getOriginalFilename() == null ? "resume" : file.getOriginalFilename().strip();
        String extension = extension(fileName);
        String contentType = CONTENT_TYPES.get(extension);
        if (contentType == null) {
            throw new ResponseStatusException(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Only PDF and DOCX files are supported");
        }

        byte[] content = read(file);
        String contentHash = sha256(content);
        if (resumeRepository.existsByJobIdAndContentHash(jobId, contentHash)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This resume was already uploaded for this job");
        }

        String rawText = extractText(content);
        String storageKey = "tenants/%s/jobs/%s/%s.%s".formatted(tenantId, jobId, UUID.randomUUID(), extension);
        storage.store(storageKey, content, contentType);

        Resume resume = new Resume(tenantId, jobId,
                new Resume.StoredFile(fileName, contentType, storageKey, contentHash),
                PiiMasker.mask(rawText),
                GithubProfiles.username(rawText).orElse(null),
                CurrentUser.requireEmail());
        try {
            return ResumeDetails.from(resumeRepository.save(resume));
        } catch (DataIntegrityViolationException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This resume was already uploaded for this job");
        }
    }

    private String extractText(byte[] content) {
        try {
            String text = textExtractor.extract(content);
            if (text.isBlank()) {
                throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_CONTENT, "No readable text found, scanned resumes are not supported");
            }
            return text;
        } catch (ResponseStatusException e) {
            throw e;
        } catch (RuntimeException e) {
            log.warn("Could not read resume text", e);
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_CONTENT, "Could not read this file");
        }
    }

    private static String extension(String fileName) {
        int dot = fileName.lastIndexOf('.');
        return dot < 0 ? "" : fileName.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    private static byte[] read(MultipartFile file) {
        try {
            return file.getBytes();
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Could not read the uploaded file");
        }
    }

    private static String sha256(byte[] content) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(content));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    record ResumeDetails(UUID id, UUID jobId, String fileName, String githubUsername, int textLength, Instant createdAt) {

        static ResumeDetails from(Resume resume) {
            return new ResumeDetails(resume.getId(), resume.getJobId(), resume.getFileName(), resume.getGithubUsername(),
                    resume.getText().length(), resume.getCreatedAt());
        }
    }
}
