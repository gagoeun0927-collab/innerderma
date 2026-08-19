package com.innerderma.airule.domain;

/**
 * Rule Engine 파이프라인 실행 순서. enum 선언 순서(ordinal)가 곧 category 실행 순서이다.
 * 정책 D 기준: SAFETY → TREATMENT_RESTRICTION → TREND → CURRENT_CONCERN → GOAL → PRODUCT_MATCHING → ROUTINE → RESPONSE_UX.
 * 기존 INPUT_IMAGE / NIGHT_CARE / MORNING_CARE 등은 해당 단계에 배치한다.
 */
public enum AiRuleCategory {
    SAFETY,
    INPUT_IMAGE,
    PROCEDURE,
    TREATMENT,
    TREND,
    SKIN_STATE,
    PRIORITY_GOAL,
    SEASON,
    ALERT,
    PIECE_SEOUL,
    WIM_INNER_CARE,
    NIGHT_CARE,
    MORNING_CARE,
    RESPONSE_UX
}
