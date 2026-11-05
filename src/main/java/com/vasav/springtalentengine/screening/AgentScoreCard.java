package com.vasav.springtalentengine.screening;

import java.util.List;

record AgentScoreCard(List<Criterion> criteria,
                      List<String> strengths,
                      List<String> gaps,
                      Recommendation recommendation,
                      String summary) {

    record Criterion(String skill, Integer score, String evidence) {
    }
}
