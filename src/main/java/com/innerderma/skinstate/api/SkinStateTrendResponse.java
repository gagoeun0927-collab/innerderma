package com.innerderma.skinstate.api;

import com.innerderma.skinstate.trend.SkinStateTrend;
import com.innerderma.skinstate.trend.TrendResult;

import java.time.LocalDate;
import java.util.Map;

public record SkinStateTrendResponse(
        SkinStateTrend overallTrend,
        Map<String, SkinStateTrend> symptomTrends,
        String scoringVersion,
        LocalDate latestDate,
        LocalDate previousDate,
        Map<String, Boolean> ruleSignals
) {
    public static SkinStateTrendResponse from(TrendResult result) {
        return new SkinStateTrendResponse(
                result.overallTrend(),
                result.symptomTrends(),
                result.scoringVersion(),
                result.latestDate(),
                result.previousDate(),
                result.toRuleSignals()
        );
    }
}
