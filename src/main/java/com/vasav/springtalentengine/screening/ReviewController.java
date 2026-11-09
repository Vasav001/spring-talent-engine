package com.vasav.springtalentengine.screening;

import com.vasav.springtalentengine.screening.ReviewService.OverrideDetails;
import com.vasav.springtalentengine.screening.ScreeningService.ScreeningDetails;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/results/{resultId}")
@RequiredArgsConstructor
class ReviewController {

    private final ReviewService reviewService;

    @PutMapping("/override")
    @PreAuthorize("hasAuthority('RESULT_OVERRIDE')")
    public ScreeningDetails override(@PathVariable UUID resultId, @Valid @RequestBody OverrideRequest request) {
        return reviewService.override(resultId, request.score(), request.reason());
    }

    @DeleteMapping("/override")
    @PreAuthorize("hasAuthority('RESULT_OVERRIDE')")
    public ScreeningDetails clear(@PathVariable UUID resultId) {
        return reviewService.clear(resultId);
    }

    @GetMapping("/overrides")
    @PreAuthorize("hasAuthority('RESULT_READ')")
    public List<OverrideDetails> history(@PathVariable UUID resultId) {
        return reviewService.history(resultId);
    }

    record OverrideRequest(@NotNull @Min(0) @Max(100) Integer score, @NotBlank @Size(max = 500) String reason) {
    }
}
