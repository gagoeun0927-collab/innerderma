package com.innerderma.airule.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AiRuleRepository extends JpaRepository<AiRule, Long> {
    boolean existsByRuleIdAndVersion(String ruleId, String version);

    List<AiRule> findByEnabledTrueOrderByPriorityDescRuleIdAsc();
}
