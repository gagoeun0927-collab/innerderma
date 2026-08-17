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

    public List<AiRule> getEnabledRules() {
        return repository.findByEnabledTrueOrderByPriorityDescRuleIdAsc();
    }
}
