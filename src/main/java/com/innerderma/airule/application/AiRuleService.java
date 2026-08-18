package com.innerderma.airule.application;

import com.innerderma.airule.domain.AiRule;
import com.innerderma.airule.domain.AiRuleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class AiRuleService {
    private final AiRuleRepository repository;

    public AiRuleService(AiRuleRepository repository) {
        this.repository = repository;
    }

    public List<AiRule> getAllRules() {
        return repository.findAll();
    }

    public List<AiRule> getEnabledRules() {
        return repository.findByEnabledTrueOrderByPriorityDescRuleIdAsc();
    }

    @Transactional
    public AiRule toggleRule(String ruleId, boolean enabled) {
        AiRule rule = repository.findByRuleId(ruleId)
                .orElseThrow(() -> new IllegalArgumentException("Rule not found: " + ruleId));
        rule.setEnabled(enabled);
        return repository.save(rule);
    }
}
