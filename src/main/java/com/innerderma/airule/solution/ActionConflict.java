package com.innerderma.airule.solution;

/**
 * action 병합 중 같은 key 에 서로 다른(숫자 아님) 값이 충돌한 사실을 보존한다.
 * 실행 순서상 먼저 적용된(높은 우선순위) 값을 유지하고, 무시된 값과 규칙을 함께 기록한다.
 */
public record ActionConflict(
        String key,
        Object appliedValue,
        Object ignoredValue,
        String appliedRuleId,
        String ignoredRuleId
) {
}
