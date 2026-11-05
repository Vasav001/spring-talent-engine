package com.vasav.springtalentengine.jobs;

import java.util.List;
import java.util.UUID;

public record JobRubric(UUID jobId, String title, JobStatus status, List<Item> items) {

    public record Item(String skill, RequirementType type, int weight, Integer minYears) {
    }
}
