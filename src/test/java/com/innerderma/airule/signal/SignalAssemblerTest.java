package com.innerderma.airule.signal;

import com.innerderma.airule.engine.RuleEvaluationContext;
import com.innerderma.selfcheck.domain.SelfCheck;
import com.innerderma.selfcheck.domain.SelfCheckRepository;
import com.innerderma.selfcheck.domain.SymptomSeverity;
import com.innerderma.skincapture.domain.SkinCaptureRepository;
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
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SignalAssemblerTest {

    private static final String USER_CODE = "WHS-DEMO-001";

    private SkinStateSnapshotRepository snapshotRepository;
    private SelfCheckRepository selfCheckRepository;
    private SkinCaptureRepository skinCaptureRepository;
    private SkinStateTrendService trendService;
    private SignalAssembler assembler;

    @BeforeEach
    void setUp() {
        snapshotRepository = mock(SkinStateSnapshotRepository.class);
        selfCheckRepository = mock(SelfCheckRepository.class);
        skinCaptureRepository = mock(SkinCaptureRepository.class);
        trendService = mock(SkinStateTrendService.class);
        assembler = new SignalAssembler(snapshotRepository, selfCheckRepository, skinCaptureRepository, trendService, new ObjectMapper());
        when(selfCheckRepository.findFirstByUser_UserCodeOrderByCheckedAtDesc(USER_CODE))
                .thenReturn(Optional.empty());
        when(skinCaptureRepository.findFirstByUser_UserCodeOrderByCapturedAtDesc(USER_CODE))
                .thenReturn(Optional.empty());
    }

    private String scores(int pain, int heat, int tight, int dry, int itch, int swell, int peel, int breakout) {
        return "{\"pain\":" + pain + ",\"heatSensation\":" + heat + ",\"tightness\":" + tight
                + ",\"dryness\":" + dry + ",\"itching\":" + itch + ",\"swelling\":" + swell
                + ",\"peeling\":" + peel + ",\"breakout\":" + breakout + "}";
    }

    private SkinStateSnapshot snapshot(String scoresJson, String dominant) {
        return new SkinStateSnapshot(new User(USER_CODE, "테스트 사용자", "010-1234-1234"),
                LocalDate.of(2026, 8, 17), "selfcheck-ordinal-v1", scoresJson, dominant, 1L, null,
                LocalDateTime.of(2026, 8, 17, 12, 30));
    }

    @Test
    void convertsTrendAndSnapshotFactsToSignals() {
        when(trendService.evaluateLatest(USER_CODE)).thenReturn(new TrendResult(
                SkinStateTrend.WORSENING, Map.of(), "selfcheck-ordinal-v1",
                LocalDate.of(2026, 8, 17), LocalDate.of(2026, 8, 16)));
        when(snapshotRepository.findFirstByUser_UserCodeOrderBySnapshotDateDesc(USER_CODE))
                .thenReturn(Optional.of(snapshot(scores(0, 0, 0, 3, 0, 0, 0, 0), "dryness")));

        RuleEvaluationContext context = assembler.assemble(USER_CODE);

        assertThat(context.isActive("trend_worsening")).isTrue();
        assertThat(context.isActive("trend_improving")).isFalse();
        assertThat(context.isActive("has_severe_symptom")).isTrue();
        assertThat(context.isActive("dominant_dryness")).isTrue();
    }

    @Test
    void hasSevereSymptomIsFalseWhenNoAxisReachesSevere() {
        when(trendService.evaluateLatest(USER_CODE)).thenReturn(new TrendResult(
                SkinStateTrend.STABLE, Map.of(), "selfcheck-ordinal-v1",
                LocalDate.of(2026, 8, 17), LocalDate.of(2026, 8, 16)));
        when(snapshotRepository.findFirstByUser_UserCodeOrderBySnapshotDateDesc(USER_CODE))
                .thenReturn(Optional.of(snapshot(scores(0, 1, 0, 2, 0, 0, 0, 0), "dryness")));

        RuleEvaluationContext context = assembler.assemble(USER_CODE);

        assertThat(context.isActive("has_severe_symptom")).isFalse();
        assertThat(context.isActive("dominant_dryness")).isTrue();
    }

    @Test
    void emitsTrendUnknownAndNoSnapshotFactsWhenNoSnapshot() {
        when(trendService.evaluateLatest(USER_CODE)).thenReturn(
                TrendResult.unknown("selfcheck-ordinal-v1", null, null));
        when(snapshotRepository.findFirstByUser_UserCodeOrderBySnapshotDateDesc(USER_CODE))
                .thenReturn(Optional.empty());

        RuleEvaluationContext context = assembler.assemble(USER_CODE);

        assertThat(context.isActive("trend_unknown")).isTrue();
        assertThat(context.isActive("has_severe_symptom")).isFalse();
        assertThat(context.isActive("requires_safety_attention")).isFalse();
        assertThat(context.signals().keySet().stream().noneMatch(key -> key.startsWith("dominant_"))).isTrue();
    }

    @Test
    void emitsSafetyAttentionWhenSelfCheckRequiresIt() {
        when(trendService.evaluateLatest(USER_CODE)).thenReturn(new TrendResult(
                SkinStateTrend.STABLE, Map.of(), "selfcheck-ordinal-v1",
                LocalDate.of(2026, 8, 17), LocalDate.of(2026, 8, 16)));
        when(snapshotRepository.findFirstByUser_UserCodeOrderBySnapshotDateDesc(USER_CODE))
                .thenReturn(Optional.empty());
        SelfCheck severeCheck = new SelfCheck(
                new User(USER_CODE, "test", "010-1234-1234"),
                LocalDateTime.of(2026, 8, 17, 12, 0),
                SymptomSeverity.SEVERE, SymptomSeverity.NONE, SymptomSeverity.NONE, SymptomSeverity.NONE,
                SymptomSeverity.NONE, SymptomSeverity.NONE, SymptomSeverity.NONE, SymptomSeverity.NONE, null);
        when(selfCheckRepository.findFirstByUser_UserCodeOrderByCheckedAtDesc(USER_CODE))
                .thenReturn(Optional.of(severeCheck));

        RuleEvaluationContext context = assembler.assemble(USER_CODE);

        assertThat(context.isActive("requires_safety_attention")).isTrue();
    }
}
