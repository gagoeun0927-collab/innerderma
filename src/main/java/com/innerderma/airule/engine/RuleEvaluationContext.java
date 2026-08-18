package com.innerderma.airule.engine;

import java.util.Map;

/**
 * Rule Engine 입력. 규칙의 conditions JSON 에 등장하는 신호 이름을 key 로, 활성 여부를 value 로 담는다.
 * 신호 이름은 규칙 원장(conditions)에 저장된 flag 이름과 그대로 일치해야 한다.
 */
public record RuleEvaluationContext(Map<String, Boolean> signals) {

    public RuleEvaluationContext {
        signals = signals == null ? Map.of() : Map.copyOf(signals);
    }

    public static RuleEvaluationContext of(Map<String, Boolean> signals) {
        return new RuleEvaluationContext(signals);
    }

    public static RuleEvaluationContext empty() {
        return new RuleEvaluationContext(Map.of());
    }

    public boolean isActive(String signal) {
        return Boolean.TRUE.equals(signals.get(signal));
    }
}
