package com.innerderma.airule.solution;

import com.innerderma.airule.domain.AiRule;
import com.innerderma.airule.domain.AiRuleCategory;
import com.innerderma.airule.domain.AiRuleRepository;
import com.innerderma.airule.engine.RuleEngine;
import com.innerderma.airule.signal.RulePipelineService;
import com.innerderma.airule.signal.SignalAssembler;
import com.innerderma.skinstate.domain.SkinStateSnapshotRepository;
import com.innerderma.skinstate.trend.SkinStateTrend;
import com.innerderma.skinstate.trend.SkinStateTrendService;
import com.innerderma.skinstate.trend.TrendResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SolutionAssemblerPipelineTest {

    private static final String USER_CODE = "WHS-DEMO-001";

    private AiRuleRepository ruleRepository;
    private SkinStateSnapshotRepository snapshotRepository;
    private SkinStateTrendService trendService;
    private SolutionAssembler assembler;

    @BeforeEach
    void setUp() {
        ruleRepository = mock(AiRuleRepository.class);
        snapshotRepository = mock(SkinStateSnapshotRepository.class);
        trendService = mock(SkinStateTrendService.class);
        ObjectMapper objectMapper = new ObjectMapper();
        SignalAssembler signalAssembler = new SignalAssembler(snapshotRepository, trendService, objectMapper);
        RuleEngine ruleEngine = new RuleEngine(ruleRepository, objectMapper);
        RulePipelineService pipeline = new RulePipelineService(signalAssembler, ruleEngine);
        assembler = new SolutionAssembler(pipeline, objectMapper);
    }

    @Test
    void assemblesSolutionFromSeedRulesForUser() {
        AiRule r000 = new AiRule("R000", AiRuleCategory.SAFETY, "Safety First", 1000,
                "{\"severe_or_unusual_symptom\":true}", "{\"recommendation_mode\":\"CAUTION\"}",
                "[\"NO_AGGRESSIVE_ROUTINE\"]", "safety", "1.0.0", true);
        AiRule r010 = new AiRule("R010", AiRuleCategory.PRIORITY_GOAL, "Minimum Intervention", 500,
                "{\"always\":true}", "{\"night_max_steps\":4,\"morning_max_steps\":3}",
                "[\"NO_UNNECESSARY_PRODUCT_ADDITION\"]", "minimum", "1.0.0", true);
        when(ruleRepository.findByEnabledTrueOrderByPriorityDescRuleIdAsc()).thenReturn(List.of(r000, r010));
        // R000 의 신호 소스(severe_or_unusual_symptom)는 없으므로 R010(always)만 발화해야 한다.
        when(trendService.evaluateLatest(USER_CODE)).thenReturn(
                new TrendResult(SkinStateTrend.STABLE, Map.of(), "selfcheck-ordinal-v1",
                        LocalDate.of(2026, 8, 17), LocalDate.of(2026, 8, 16)));
        when(snapshotRepository.findFirstByUser_UserCodeOrderBySnapshotDateDesc(USER_CODE))
                .thenReturn(Optional.empty());

        SolutionObject solution = assembler.assembleForUser(USER_CODE);

        assertThat(solution.appliedRules()).containsExactly("R010@1.0.0");
        assertThat(solution.actions()).containsEntry("night_max_steps", 4).containsEntry("morning_max_steps", 3);
        assertThat(solution.restrictions()).containsExactly("NO_UNNECESSARY_PRODUCT_ADDITION");
        assertThat(solution.explanationTemplates()).containsEntry("R010", "minimum").doesNotContainKey("R000");
        assertThat(solution.sourceSignals()).containsEntry("trend_stable", true);
    }
}
