package com.innerderma.skinstate.trend;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Trend 판정 결과. 전체 trend 와 증상별 trend 를 함께 제공하고, Rule Engine 이 쓸 수 있는
 * boolean 신호 맵(trend_improving/trend_stable/trend_worsening/trend_unknown)으로 변환할 수 있다.
 */
public record TrendResult(
        SkinStateTrend overallTrend,
        Map<String, SkinStateTrend> symptomTrends,
        String scoringVersion,
        LocalDate latestDate,
        LocalDate previousDate
) {
    public TrendResult {
        symptomTrends = symptomTrends == null ? Map.of() : Map.copyOf(symptomTrends);
    }

    public static TrendResult unknown(String scoringVersion, LocalDate latestDate, LocalDate previousDate) {
        return new TrendResult(SkinStateTrend.UNKNOWN, Map.of(), scoringVersion, latestDate, previousDate);
    }

    /** 전체 trend 를 Rule Engine 신호로 변환한다. 해당 신호만 true, 나머지는 false. */
    public Map<String, Boolean> toRuleSignals() {
        Map<String, Boolean> signals = new LinkedHashMap<>();
        for (SkinStateTrend trend : SkinStateTrend.values()) {
            signals.put(trend.signalName(), trend == overallTrend);
        }
        return signals;
    }
}
