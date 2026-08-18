package com.innerderma.airule.signal;

import com.innerderma.airule.domain.AiRule;
import com.innerderma.airule.domain.AiRuleCategory;
import com.innerderma.airule.domain.AiRuleRepository;
import com.innerderma.airule.engine.RuleEngine;
import com.innerderma.airule.engine.RuleEvaluationResult;
import com.innerderma.skinstate.domain.SkinStateSnapshot;
import com.innerderma.skinstate.domain.SkinStateSnapshotRepository;
import com.innerderma.skinstate.trend.SkinStateTrend;
import com.innerderma.skinstate.trend.SkinStateTrendService;
import com.innerderma.skinstate.trend.TrendResult;
import com.innerderma.user.domain.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RulePipelineServiceTest {

    private static final String USER_CODE = "WHS-DEMO-001";

    private AiRuleRepository ruleRepository;
    private SkinStateSnapshotRepository snapshotRepository;
    private SkinStateTrendService trendService;
    private RulePipelineService pipeline;

    @BeforeEach
    void setUp() {
        ruleRepository = mock(AiRuleRepository.class);
        snapshotRepository = mock(SkinStateSnapshotRepository.class);
        trendService = mock(SkinStateTrendService.class);
        ObjectMapper objectMapper = new ObjectMapper();
        SignalAssembler assembler = new SignalAssembler(snapshotRepository, trendService, objectMapper);
        RuleEngine ruleEngine = new RuleEngine(ruleRepository, objectMapper);
        pipeline = new RulePipelineService(assembler, ruleEngine);
    }

    private AiRule rule(String ruleId, AiRuleCategory category, int priority, String conditions) {
        return new AiRule(ruleId, category, ruleId, priority, conditions, "{}", "[]", null, "1.0.0", true);
    }

    private List<AiRule> seedRules() {
        return List.of(
                rule("R000", AiRuleCategory.SAFETY, 1000,
                        "{\"severe_or_unusual_symptom\":true,\"or_rapidly_worsening\":true,\"or_professional_review_required\":true}"),
                rule("R002", AiRuleCategory.INPUT_IMAGE, 900,
                        "{\"face_not_detected\":true,\"or_image_blurry\":true,\"or_lighting_insufficient\":true,\"or_face_partially_occluded\":true}"),
                rule("R010", AiRuleCategory.PRIORITY_GOAL, 500, "{\"always\":true}"));
    }

    private String scores(int dry) {
        return "{\"pain\":0,\"heatSensation\":0,\"tightness\":0,\"dryness\":" + dry
                + ",\"itching\":0,\"swelling\":0,\"peeling\":0,\"breakout\":0}";
    }

    @Test
    void firesOnlyAlwaysRuleAndDoesNotMisfireSourcelessRules() {
        when(ruleRepository.findByEnabledTrueOrderByPriorityDescRuleIdAsc()).thenReturn(seedRules());
        // trend_worsening + has_severe_symptom 이 켜져도 R000/R002 의 조건 키와 이름이 다르므로 발화하면 안 된다.
        when(trendService.evaluateLatest(USER_CODE)).thenReturn(new TrendResult(
                SkinStateTrend.WORSENING, Map.of(), "selfcheck-ordinal-v1",
                LocalDate.of(2026, 8, 17), LocalDate.of(2026, 8, 16)));
        when(snapshotRepository.findFirstByUser_UserCodeOrderBySnapshotDateDesc(USER_CODE))
                .thenReturn(Optional.of(new SkinStateSnapshot(
                        new User(USER_CODE, "테스트 사용자", "010-1234-1234"),
                        LocalDate.of(2026, 8, 17), "selfcheck-ordinal-v1", scores(3), "dryness", 1L, null,
                        LocalDateTime.of(2026, 8, 17, 12, 30))));

        RuleEvaluationResult result = pipeline.evaluateForUser(USER_CODE);

        assertThat(result.firedRuleIds()).containsExactly("R010");
        assertThat(result.hasFired("R000")).isFalse();
        assertThat(result.hasFired("R002")).isFalse();
    }

    @Test
    void stillFiresAlwaysRuleWhenNoSnapshotAndUnknownTrend() {
        when(ruleRepository.findByEnabledTrueOrderByPriorityDescRuleIdAsc()).thenReturn(seedRules());
        when(trendService.evaluateLatest(USER_CODE))
                .thenReturn(TrendResult.unknown("selfcheck-ordinal-v1", null, null));
        when(snapshotRepository.findFirstByUser_UserCodeOrderBySnapshotDateDesc(USER_CODE))
                .thenReturn(Optional.empty());

        RuleEvaluationResult result = pipeline.evaluateForUser(USER_CODE);

        assertThat(result.firedRuleIds()).containsExactly("R010");
    }
}
