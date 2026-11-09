package com.vasav.springtalentengine.screening;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

interface ScreeningResultRepository extends JpaRepository<ScreeningResult, UUID> {

    Optional<ScreeningResult> findByResumeId(UUID resumeId);

    Page<ScreeningResult> findByTenantIdAndJobId(UUID tenantId, UUID jobId, Pageable pageable);

    Page<ScreeningResult> findByTenantIdAndJobIdAndStatus(UUID tenantId, UUID jobId, ScreeningStatus status, Pageable pageable);

    @Query("""
            select count(r) as screened, coalesce(sum(r.tokensIn), 0) as tokensIn, coalesce(sum(r.tokensOut), 0) as tokensOut
            from ScreeningResult r
            where r.tenantId = :tenantId and r.status = :status and r.screenedAt >= :since
            """)
    TokenTotals totalsSince(@Param("tenantId") UUID tenantId, @Param("status") ScreeningStatus status, @Param("since") Instant since);

    interface TokenTotals {

        long getScreened();

        long getTokensIn();

        long getTokensOut();
    }
}
