package com.vasav.springtalentengine.screening;

import com.vasav.springtalentengine.ingestion.ResumeContent;
import com.vasav.springtalentengine.ingestion.ResumeContents;
import com.vasav.springtalentengine.ingestion.ResumeProcessing;
import com.vasav.springtalentengine.ingestion.ResumeRejectedException;
import com.vasav.springtalentengine.jobs.JobRubric;
import com.vasav.springtalentengine.jobs.JobRubrics;
import com.vasav.springtalentengine.jobs.SkillSynonyms;
import com.vasav.springtalentengine.screening.KeywordGate.KeywordMatch;
import com.vasav.springtalentengine.shared.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
class ScreeningService {

    private final ScreeningResultRepository resultRepository;
    private final ResumeContents resumeContents;
    private final ResumeProcessing resumeProcessing;
    private final JobRubrics jobRubrics;
    private final SkillSynonyms skillSynonyms;
    private final ScreeningAgent screeningAgent;
    private final TransactionTemplate transactionTemplate;

    ScreeningDetails screen(UUID resumeId, boolean force) {
        ResumeContent resume = resumeContents.find(resumeId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Resume not found"));
        JobRubric rubric = jobRubrics.find(resume.jobId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Job not found"));
        return save(resume, evaluate(rubric, resume, force, true));
    }

    void screenQueued(UUID resumeId, boolean lastAttempt) {
        try {
            Optional<ResumeContent> resume = resumeProcessing.prepare(resumeId);
            if (resume.isEmpty()) {
                return;
            }
            JobRubric rubric = jobRubrics.find(resume.get().jobId())
                    .orElseThrow(() -> new ResumeRejectedException("Job not found"));
            ScreeningOutcome outcome = evaluate(rubric, resume.get(), false, lastAttempt);
            save(resume.get(), outcome);
            resumeProcessing.finish(resumeId, outcome.error());
        } catch (ResumeRejectedException e) {
            resumeProcessing.finish(resumeId, e.getMessage());
        } catch (RuntimeException e) {
            if (!lastAttempt) {
                throw e;
            }
            log.warn("Giving up on resume {}", resumeId, e);
            resumeProcessing.finish(resumeId, "Processing failed after several attempts");
        }
    }

    @Transactional(readOnly = true)
    Page<ResultSummary> results(UUID jobId, ScreeningStatus status, Pageable pageable) {
        UUID tenantId = TenantContext.require();
        Page<ScreeningResult> results = status == null
                ? resultRepository.findByTenantIdAndJobId(tenantId, jobId, pageable)
                : resultRepository.findByTenantIdAndJobIdAndStatus(tenantId, jobId, status, pageable);
        return results.map(ResultSummary::from);
    }

    @Transactional(readOnly = true)
    ScreeningDetails get(UUID resultId) {
        return resultRepository.findById(resultId)
                .filter(result -> result.getTenantId().equals(TenantContext.require()))
                .map(ScreeningDetails::from)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Result not found"));
    }

    private ScreeningOutcome evaluate(JobRubric rubric, ResumeContent resume, boolean force, boolean recordFailures) {
        KeywordMatch keywords = KeywordGate.evaluate(rubric.items(), resume.text(), skillSynonyms.load());
        if (!keywords.passes() && !force) {
            return ScreeningOutcome.lowMatch(keywords);
        }
        try {
            return ScreeningOutcome.screened(keywords, rubric.items(), screeningAgent.screen(rubric, resume));
        } catch (RuntimeException e) {
            if (!recordFailures) {
                throw e;
            }
            log.warn("AI screening failed for resume {}", resume.resumeId(), e);
            return ScreeningOutcome.failed(keywords, "AI screening failed, try again later");
        }
    }

    private ScreeningDetails save(ResumeContent resume, ScreeningOutcome outcome) {
        UUID tenantId = TenantContext.require();
        return transactionTemplate.execute(status -> {
            ScreeningResult result = resultRepository.findByResumeId(resume.resumeId())
                    .orElseGet(() -> new ScreeningResult(tenantId, resume.resumeId(), resume.jobId(), resume.fileName()));
            result.record(outcome);
            return ScreeningDetails.from(resultRepository.save(result));
        });
    }

    record ResultSummary(UUID id, UUID resumeId, String fileName, ScreeningStatus status, int keywordScore,
                         Integer aiScore, int finalScore, Recommendation recommendation, Instant screenedAt) {

        static ResultSummary from(ScreeningResult result) {
            return new ResultSummary(result.getId(), result.getResumeId(), result.getFileName(), result.getStatus(),
                    result.getKeywordScore(), result.getAiScore(), result.getFinalScore(), result.getRecommendation(),
                    result.getScreenedAt());
        }
    }

    record ScreeningDetails(UUID id, UUID resumeId, UUID jobId, String fileName, ScreeningStatus status,
                            int keywordScore, Integer aiScore, int finalScore, Recommendation recommendation,
                            ScreeningReport report, String error, String model, int tokensIn, int tokensOut,
                            Instant screenedAt) {

        static ScreeningDetails from(ScreeningResult result) {
            return new ScreeningDetails(result.getId(), result.getResumeId(), result.getJobId(), result.getFileName(),
                    result.getStatus(), result.getKeywordScore(), result.getAiScore(), result.getFinalScore(),
                    result.getRecommendation(), result.getReport(), result.getError(), result.getModel(),
                    result.getTokensIn(), result.getTokensOut(), result.getScreenedAt());
        }
    }
}
