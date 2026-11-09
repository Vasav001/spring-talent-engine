package com.vasav.springtalentengine.ingestion;

import com.vasav.springtalentengine.ingestion.BatchService.BatchCreated;
import com.vasav.springtalentengine.ingestion.BatchService.BatchDetails;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
class BatchController {

    private final BatchService batchService;
    private final BatchProgressStream progressStream;

    @PostMapping("/jobs/{jobId}/batches")
    @PreAuthorize("hasAuthority('BATCH_UPLOAD')")
    public ResponseEntity<BatchCreated> create(@PathVariable UUID jobId, @Valid @RequestBody CreateBatchRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(batchService.create(jobId, request.fileNames()));
    }

    @PostMapping("/batches/{batchId}/submit")
    @PreAuthorize("hasAuthority('BATCH_UPLOAD')")
    public BatchDetails submit(@PathVariable UUID batchId) {
        return batchService.submit(batchId);
    }

    @GetMapping("/batches/{batchId}")
    @PreAuthorize("hasAuthority('RESULT_READ')")
    public BatchDetails get(@PathVariable UUID batchId) {
        return batchService.get(batchId);
    }

    @GetMapping(path = "/batches/{batchId}/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @PreAuthorize("hasAuthority('RESULT_READ')")
    public SseEmitter events(@PathVariable UUID batchId) {
        return progressStream.open(batchId);
    }

    record CreateBatchRequest(@NotEmpty @Size(max = BatchService.MAX_FILES) List<@NotBlank @Size(max = 255) String> fileNames) {
    }
}
