package com.vasav.springtalentengine.screening;

import java.util.List;

public record ScreeningReport(List<String> matchedSkills,
                              List<String> missingMustSkills,
                              List<CriterionResult> criteria,
                              List<String> strengths,
                              List<String> gaps,
                              String summary) {

    public record CriterionResult(String skill, int score, String evidence, boolean verified) {
    }
}
