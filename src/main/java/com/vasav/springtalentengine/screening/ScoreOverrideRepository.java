package com.vasav.springtalentengine.screening;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

interface ScoreOverrideRepository extends JpaRepository<ScoreOverride, UUID> {

    List<ScoreOverride> findByResultIdOrderByCreatedAtDesc(UUID resultId);
}
