package com.innerderma.airule.application;

import com.innerderma.airule.domain.*;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class AiRuleServiceTest {
    private final AiRuleRepository repository = mock(AiRuleRepository.class);
    private final AiRuleService service = new AiRuleService(repository);

    @Test
    void returnsOnlyEnabledRulesInRepositoryPriorityOrder() {
        AiRule safety = new AiRule("R000", AiRuleCategory.SAFETY, "Safety First", 1000,
                "{}", "{}", "[]", null, "1.0.0", true);
        AiRule minimum = new AiRule("R010", AiRuleCategory.PRIORITY_GOAL, "Minimum Intervention", 500,
                "{}", "{}", "[]", null, "1.0.0", true);
        when(repository.findByEnabledTrueOrderByPriorityDescRuleIdAsc()).thenReturn(List.of(safety, minimum));

        assertThat(service.getEnabledRules()).extracting(AiRule::getRuleId)
                .containsExactly("R000", "R010");
        verify(repository).findByEnabledTrueOrderByPriorityDescRuleIdAsc();
    }
}
