package com.innerderma.airule.engine;

import com.innerderma.airule.domain.AiRule;
import com.innerderma.airule.domain.AiRuleCategory;
import com.innerderma.airule.domain.AiRuleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RuleEngineTest {

    private AiRuleRepository repository;
    private RuleEngine engine;

    @BeforeEach
    void setUp() {
        repository = mock(AiRuleRepository.class);
        engine = new RuleEngine(repository, new ObjectMapper());
    }

    private AiRule rule(String ruleId, AiRuleCategory category, int priority, String conditions) {
        return new AiRule(ruleId, category, ruleId, priority, conditions, "{}", "[]", null, "1.0.0", true);
    }

    @Test
    void firesAlwaysRuleRegardlessOfContext() {
        AiRule minimum = rule("R010", AiRuleCategory.PRIORITY_GOAL, 500, "{\"always\":true}");
        when(repository.findByEnabledTrueOrderByPriorityDescRuleIdAsc()).thenReturn(List.of(minimum));

        RuleEvaluationResult result = engine.evaluate(RuleEvaluationContext.empty());

        assertThat(result.firedRuleIds()).containsExactly("R010");
    }

    @Test
    void firesWhenAnyListedSignalIsActive() {
        AiRule safety = rule("R000", AiRuleCategory.SAFETY, 1000,
                "{\"severe_or_unusual_symptom\":true,\"or_rapidly_worsening\":true,\"or_professional_review_required\":true}");
        when(repository.findByEnabledTrueOrderByPriorityDescRuleIdAsc()).thenReturn(List.of(safety));

        RuleEvaluationResult result = engine.evaluate(
                RuleEvaluationContext.of(Map.of("or_rapidly_worsening", true)));

        assertThat(result.hasFired("R000")).isTrue();
    }

    @Test
    void doesNotFireWhenNoListedSignalIsActive() {
        AiRule imageGate = rule("R002", AiRuleCategory.INPUT_IMAGE, 900,
                "{\"face_not_detected\":true,\"or_image_blurry\":true}");
        when(repository.findByEnabledTrueOrderByPriorityDescRuleIdAsc()).thenReturn(List.of(imageGate));

        RuleEvaluationResult result = engine.evaluate(
                RuleEvaluationContext.of(Map.of("lighting_ok", true)));

        assertThat(result.firedRules()).isEmpty();
    }

    @Test
    void ordersFiredRulesByCategoryPipelineThenPriority() {
        AiRule minimum = rule("R010", AiRuleCategory.PRIORITY_GOAL, 500, "{\"always\":true}");
        AiRule imageGate = rule("R002", AiRuleCategory.INPUT_IMAGE, 900, "{\"always\":true}");
        AiRule safety = rule("R000", AiRuleCategory.SAFETY, 1000, "{\"always\":true}");
        // 저장소는 priority 내림차순으로 주지만 엔진은 파이프라인(category 선언 순서)으로 재정렬해야 한다.
        when(repository.findByEnabledTrueOrderByPriorityDescRuleIdAsc())
                .thenReturn(List.of(safety, imageGate, minimum));

        RuleEvaluationResult result = engine.evaluate(RuleEvaluationContext.empty());

        assertThat(result.firedRuleIds()).containsExactly("R000", "R002", "R010");
    }

    @Test
    void ordersByPriorityDescendingWithinSameCategory() {
        AiRule high = rule("R000", AiRuleCategory.SAFETY, 1000, "{\"always\":true}");
        AiRule low = rule("R021", AiRuleCategory.SAFETY, 700, "{\"always\":true}");
        when(repository.findByEnabledTrueOrderByPriorityDescRuleIdAsc())
                .thenReturn(List.of(low, high));

        RuleEvaluationResult result = engine.evaluate(RuleEvaluationContext.empty());

        assertThat(result.firedRuleIds()).containsExactly("R000", "R021");
    }

    @Test
    void skipsRuleWithMalformedConditionsJson() {
        AiRule broken = rule("R099", AiRuleCategory.SKIN_STATE, 100, "{not-valid-json");
        when(repository.findByEnabledTrueOrderByPriorityDescRuleIdAsc()).thenReturn(List.of(broken));

        RuleEvaluationResult result = engine.evaluate(RuleEvaluationContext.empty());

        assertThat(result.firedRules()).isEmpty();
    }
}
