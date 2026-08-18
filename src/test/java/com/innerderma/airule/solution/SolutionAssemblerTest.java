package com.innerderma.airule.solution;

import com.innerderma.airule.domain.AiRule;
import com.innerderma.airule.domain.AiRuleCategory;
import com.innerderma.airule.engine.RuleEvaluationContext;
import com.innerderma.airule.signal.RulePipelineService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class SolutionAssemblerTest {

    private SolutionAssembler assembler;

    @BeforeEach
    void setUp() {
        assembler = new SolutionAssembler(mock(RulePipelineService.class), new ObjectMapper());
    }

    private AiRule rule(String ruleId, AiRuleCategory category, int priority,
                        String actions, String restrictions, String explanation) {
        return new AiRule(ruleId, category, ruleId, priority, "{}", actions, restrictions,
                explanation, "1.0.0", true);
    }

    @Test
    void recordsAppliedRulesAsRuleIdAtVersionInOrder() {
        List<AiRule> fired = List.of(
                rule("R000", AiRuleCategory.SAFETY, 1000, "{}", "[]", "safety"),
                rule("R010", AiRuleCategory.PRIORITY_GOAL, 500, "{}", "[]", null));

        SolutionObject solution = assembler.assemble(fired, RuleEvaluationContext.empty());

        assertThat(solution.appliedRules()).containsExactly("R000@1.0.0", "R010@1.0.0");
        assertThat(solution.explanationTemplates()).containsEntry("R000", "safety").doesNotContainKey("R010");
    }

    @Test
    void unionsAndDeduplicatesRestrictions() {
        List<AiRule> fired = List.of(
                rule("R000", AiRuleCategory.SAFETY, 1000, "{}", "[\"NO_AGGRESSIVE_ROUTINE\",\"MINIMIZE_PROMO\"]", null),
                rule("R010", AiRuleCategory.PRIORITY_GOAL, 500, "{}", "[\"MINIMIZE_PROMO\",\"NO_UNNECESSARY_PRODUCT_ADDITION\"]", null));

        SolutionObject solution = assembler.assemble(fired, RuleEvaluationContext.empty());

        assertThat(solution.restrictions())
                .containsExactly("NO_AGGRESSIVE_ROUTINE", "MINIMIZE_PROMO", "NO_UNNECESSARY_PRODUCT_ADDITION");
    }

    @Test
    void appliesMostRestrictiveNumericLimit() {
        List<AiRule> fired = List.of(
                rule("R010", AiRuleCategory.PRIORITY_GOAL, 500, "{\"night_max_steps\":4}", "[]", null),
                rule("R011", AiRuleCategory.NIGHT_CARE, 400, "{\"night_max_steps\":2}", "[]", null));

        SolutionObject solution = assembler.assemble(fired, RuleEvaluationContext.empty());

        assertThat(solution.actions()).containsEntry("night_max_steps", 2);
        assertThat(solution.conflicts()).isEmpty();
    }

    @Test
    void keepsHigherPriorityValueAndPreservesConflictForNonNumericClash() {
        List<AiRule> fired = List.of(
                rule("R000", AiRuleCategory.SAFETY, 1000, "{\"recommendation_mode\":\"CAUTION\"}", "[]", null),
                rule("R009", AiRuleCategory.PRIORITY_GOAL, 500, "{\"recommendation_mode\":\"NORMAL\"}", "[]", null));

        SolutionObject solution = assembler.assemble(fired, RuleEvaluationContext.empty());

        assertThat(solution.actions()).containsEntry("recommendation_mode", "CAUTION");
        assertThat(solution.conflicts()).hasSize(1);
        ActionConflict conflict = solution.conflicts().get(0);
        assertThat(conflict.key()).isEqualTo("recommendation_mode");
        assertThat(conflict.appliedValue()).isEqualTo("CAUTION");
        assertThat(conflict.ignoredValue()).isEqualTo("NORMAL");
        assertThat(conflict.appliedRuleId()).isEqualTo("R000");
        assertThat(conflict.ignoredRuleId()).isEqualTo("R009");
    }

    @Test
    void preservesSourceSignalsAsMetadata() {
        SolutionObject solution = assembler.assemble(List.of(),
                RuleEvaluationContext.of(Map.of("trend_worsening", true, "has_severe_symptom", false)));

        assertThat(solution.sourceSignals()).containsEntry("trend_worsening", true)
                .containsEntry("has_severe_symptom", false);
        assertThat(solution.appliedRules()).isEmpty();
    }
}
