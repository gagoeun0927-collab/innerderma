package com.innerderma.airule.api;

import com.innerderma.airule.domain.AiRule;
import com.innerderma.airule.domain.AiRuleCategory;

public record AiRuleResponse(String ruleId, AiRuleCategory category, String name, int priority,
                             String conditionsJson, String actionsJson, String restrictionsJson,
                             String explanationTemplate, String version) {
    public static AiRuleResponse from(AiRule rule) {
        return new AiRuleResponse(rule.getRuleId(), rule.getCategory(), rule.getName(), rule.getPriority(),
                rule.getConditionsJson(), rule.getActionsJson(), rule.getRestrictionsJson(),
                rule.getExplanationTemplate(), rule.getVersion());
    }
}
