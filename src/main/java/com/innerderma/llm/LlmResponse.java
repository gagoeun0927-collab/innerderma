package com.innerderma.llm;

import java.util.List;

/**
 * LLM이 생성한 사용자 대면 응답 (§36 스키마).
 * Rule Engine의 결정을 사용자가 이해하기 좋은 자연어로 표현한 결과다.
 * LLM은 이 구조 안에서만 작성하며, 제품/루틴/안전 상태를 임의로 변경할 수 없다.
 */
public record LlmResponse(
        String headline,
        String skinStateSummary,
        String todayGoal,
        NightCare night,
        MorningCare morning,
        InnerCare innerCare,
        String caution
) {
    public record NightCare(String purpose, List<Step> steps) {}
    public record MorningCare(String purpose, List<Step> steps) {}
    public record InnerCare(List<Recommendation> recommended, List<String> avoid) {}
    public record Step(int step, String productId, String productName, String usage, String reason) {}
    public record Recommendation(String productId, String productName, String usage, String reason) {}
}
