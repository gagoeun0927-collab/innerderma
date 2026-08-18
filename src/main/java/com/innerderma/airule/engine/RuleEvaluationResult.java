package com.innerderma.airule.engine;

import com.innerderma.airule.domain.AiRule;

import java.util.List;

/**
 * Rule Engine 평가 결과. 발화(fire)한 규칙을 파이프라인 순서(category 선언 순서) 후 priority 내림차순으로 담는다.
 */
public record RuleEvaluationResult(List<AiRule> firedRules) {

    public RuleEvaluationResult {
        firedRules = firedRules == null ? List.of() : List.copyOf(firedRules);
    }

    public List<String> firedRuleIds() {
        return firedRules.stream().map(AiRule::getRuleId).toList();
    }

    public boolean hasFired(String ruleId) {
        return firedRules.stream().anyMatch(rule -> rule.getRuleId().equals(ruleId));
    }
}
