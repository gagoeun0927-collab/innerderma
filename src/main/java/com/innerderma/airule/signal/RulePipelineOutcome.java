package com.innerderma.airule.signal;

import com.innerderma.airule.engine.RuleEvaluationContext;
import com.innerderma.airule.engine.RuleEvaluationResult;

/** 파이프라인 1회 실행 결과: 사용된 신호 컨텍스트와 발화 규칙을 함께 담는다. */
public record RulePipelineOutcome(RuleEvaluationContext context, RuleEvaluationResult result) {
}
