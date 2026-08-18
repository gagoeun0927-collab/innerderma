package com.innerderma.airule.solution;

import java.util.List;
import java.util.Map;

/**
 * 비영속(in-memory) 내부 마스터 솔루션 객체. 발화 규칙의 actions/restrictions 만 결정적으로 병합한 결과이며,
 * 규칙에 없는 의료 판단/효능/사용량/제품 적합성 등을 새로 만들지 않는다. 사용자 대면 문구는 생성하지 않고
 * 규칙이 제공한 explanationTemplate 만 보존한다.
 */
public record SolutionObject(
        Map<String, Object> actions,
        List<String> restrictions,
        List<String> appliedRules,
        List<ActionConflict> conflicts,
        Map<String, String> explanationTemplates,
        Map<String, Boolean> sourceSignals
) {
    public SolutionObject {
        actions = actions == null ? Map.of() : Map.copyOf(actions);
        restrictions = restrictions == null ? List.of() : List.copyOf(restrictions);
        appliedRules = appliedRules == null ? List.of() : List.copyOf(appliedRules);
        conflicts = conflicts == null ? List.of() : List.copyOf(conflicts);
        explanationTemplates = explanationTemplates == null ? Map.of() : Map.copyOf(explanationTemplates);
        sourceSignals = sourceSignals == null ? Map.of() : Map.copyOf(sourceSignals);
    }
}
