package com.vasav.springtalentengine.ingestion;

import com.vasav.springtalentengine.ingestion.ResumeService.ResumeDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
class ResumeController {

    private final ResumeService resumeService;

    @PostMapping(path = "/jobs/{jobId}/resumes", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('BATCH_UPLOAD')")
    public ResponseEntity<ResumeDetails> upload(@PathVariable UUID jobId, @RequestParam("file") MultipartFile file) {
        return ResponseEntity.status(HttpStatus.CREATED).body(resumeService.upload(jobId, file));
    }
}
