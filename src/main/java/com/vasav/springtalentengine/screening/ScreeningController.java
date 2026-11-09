package com.vasav.springtalentengine.screening;

import com.vasav.springtalentengine.screening.ScreeningService.ResultSummary;
import com.vasav.springtalentengine.screening.ScreeningService.ScreeningDetails;
import com.vasav.springtalentengine.screening.ScreeningService.UsageDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
class ScreeningController {

    private final ScreeningService screeningService;

    @PostMapping("/resumes/{resumeId}/screen")
    @PreAuthorize("hasAuthority('BATCH_UPLOAD')")
    public ScreeningDetails screen(@PathVariable UUID resumeId, @RequestParam(defaultValue = "false") boolean force) {
        return screeningService.screen(resumeId, force);
    }

    @GetMapping("/jobs/{jobId}/results")
    @PreAuthorize("hasAuthority('RESULT_READ')")
    public Page<ResultSummary> results(@PathVariable UUID jobId,
                                       @RequestParam(required = false) ScreeningStatus status,
                                       @PageableDefault(size = 20, sort = "finalScore", direction = Sort.Direction.DESC) Pageable pageable) {
        return screeningService.results(jobId, status, pageable);
    }

    @GetMapping("/usage")
    @PreAuthorize("hasAuthority('RESULT_READ')")
    public UsageDetails usage() {
        return screeningService.usage();
    }

    @GetMapping("/results/{resultId}")
    @PreAuthorize("hasAuthority('RESULT_READ')")
    public ScreeningDetails get(@PathVariable UUID resultId) {
        return screeningService.get(resultId);
    }
}
