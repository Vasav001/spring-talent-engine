package com.vasav.springtalentengine.screening;

import com.vasav.springtalentengine.screening.ScreeningService.ScreeningDetails;
import com.vasav.springtalentengine.shared.CurrentUser;
import com.vasav.springtalentengine.shared.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
class ReviewService {

    private final ScreeningResultRepository resultRepository;
    private final ScoreOverrideRepository overrideRepository;

    @Transactional
    ScreeningDetails override(UUID resultId, int score, String reason) {
        ScreeningResult result = find(resultId);
        int previousScore = result.getRankScore();
        String reviewer = CurrentUser.requireEmail();
        result.override(score, reason.strip(), reviewer);
        overrideRepository.save(new ScoreOverride(result, previousScore, reviewer));
        return ScreeningDetails.from(result);
    }

    @Transactional
    ScreeningDetails clear(UUID resultId) {
        ScreeningResult result = find(resultId);
        if (result.getOverrideScore() != null) {
            int previousScore = result.getRankScore();
            result.clearOverride();
            overrideRepository.save(new ScoreOverride(result, previousScore, CurrentUser.requireEmail()));
        }
        return ScreeningDetails.from(result);
    }

    @Transactional(readOnly = true)
    List<OverrideDetails> history(UUID resultId) {
        find(resultId);
        return overrideRepository.findByResultIdOrderByCreatedAtDesc(resultId).stream().map(OverrideDetails::from).toList();
    }

    private ScreeningResult find(UUID resultId) {
        return resultRepository.findById(resultId)
                .filter(result -> result.getTenantId().equals(TenantContext.require()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Result not found"));
    }

    record OverrideDetails(int previousScore, Integer newScore, String reason, String reviewerEmail, Instant createdAt) {

        static OverrideDetails from(ScoreOverride override) {
            return new OverrideDetails(override.getPreviousScore(), override.getNewScore(), override.getReason(),
                    override.getReviewerEmail(), override.getCreatedAt());
        }
    }
}
