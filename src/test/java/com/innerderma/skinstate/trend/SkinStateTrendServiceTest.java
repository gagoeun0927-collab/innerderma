package com.innerderma.skinstate.trend;

import com.innerderma.common.error.BusinessException;
import com.innerderma.common.error.ErrorCode;
import com.innerderma.skinstate.domain.SkinStateSnapshot;
import com.innerderma.skinstate.domain.SkinStateSnapshotRepository;
import com.innerderma.user.domain.User;
import com.innerderma.user.domain.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SkinStateTrendServiceTest {

    private static final String USER_CODE = "WHS-DEMO-001";
    private static final String VERSION = "selfcheck-ordinal-v1";

    private SkinStateSnapshotRepository snapshotRepository;
    private UserRepository userRepository;
    private SkinStateTrendService service;

    @BeforeEach
    void setUp() {
        snapshotRepository = mock(SkinStateSnapshotRepository.class);
        userRepository = mock(UserRepository.class);
        service = new SkinStateTrendService(snapshotRepository, userRepository, new ObjectMapper());
        when(userRepository.existsByUserCode(USER_CODE)).thenReturn(true);
    }

    private User user() {
        return new User(USER_CODE, "테스트 사용자", "010-1234-1234");
    }

    private String scores(int pain, int heat, int tight, int dry, int itch, int swell, int peel, int breakout) {
        return "{\"pain\":" + pain + ",\"heatSensation\":" + heat + ",\"tightness\":" + tight
                + ",\"dryness\":" + dry + ",\"itching\":" + itch + ",\"swelling\":" + swell
                + ",\"peeling\":" + peel + ",\"breakout\":" + breakout + "}";
    }

    private SkinStateSnapshot snap(LocalDate date, String version, String scoresJson) {
        return new SkinStateSnapshot(user(), date, version, scoresJson, null, 1L, null,
                LocalDateTime.of(2026, 8, 17, 12, 30));
    }

    @Test
    void reportsWorseningWhenScoresIncrease() {
        SkinStateSnapshot latest = snap(LocalDate.of(2026, 8, 17), VERSION, scores(0, 0, 0, 3, 0, 0, 0, 0));
        SkinStateSnapshot previous = snap(LocalDate.of(2026, 8, 16), VERSION, scores(0, 0, 0, 1, 0, 0, 0, 0));
        when(snapshotRepository.findTop2ByUser_UserCodeOrderBySnapshotDateDesc(USER_CODE))
                .thenReturn(List.of(latest, previous));

        TrendResult result = service.evaluateLatest(USER_CODE);

        assertThat(result.overallTrend()).isEqualTo(SkinStateTrend.WORSENING);
        assertThat(result.symptomTrends()).containsEntry("dryness", SkinStateTrend.WORSENING)
                .containsEntry("pain", SkinStateTrend.STABLE);
        assertThat(result.latestDate()).isEqualTo(LocalDate.of(2026, 8, 17));
        assertThat(result.previousDate()).isEqualTo(LocalDate.of(2026, 8, 16));
    }

    @Test
    void reportsImprovingWhenScoresDecrease() {
        SkinStateSnapshot latest = snap(LocalDate.of(2026, 8, 17), VERSION, scores(0, 0, 0, 1, 0, 0, 0, 0));
        SkinStateSnapshot previous = snap(LocalDate.of(2026, 8, 16), VERSION, scores(0, 0, 0, 3, 0, 0, 0, 0));
        when(snapshotRepository.findTop2ByUser_UserCodeOrderBySnapshotDateDesc(USER_CODE))
                .thenReturn(List.of(latest, previous));

        TrendResult result = service.evaluateLatest(USER_CODE);

        assertThat(result.overallTrend()).isEqualTo(SkinStateTrend.IMPROVING);
        assertThat(result.symptomTrends()).containsEntry("dryness", SkinStateTrend.IMPROVING);
        assertThat(result.toRuleSignals()).containsEntry("trend_improving", true)
                .containsEntry("trend_worsening", false);
    }

    @Test
    void reportsStableWhenScoresUnchanged() {
        SkinStateSnapshot latest = snap(LocalDate.of(2026, 8, 17), VERSION, scores(0, 1, 0, 2, 0, 0, 0, 0));
        SkinStateSnapshot previous = snap(LocalDate.of(2026, 8, 16), VERSION, scores(0, 1, 0, 2, 0, 0, 0, 0));
        when(snapshotRepository.findTop2ByUser_UserCodeOrderBySnapshotDateDesc(USER_CODE))
                .thenReturn(List.of(latest, previous));

        TrendResult result = service.evaluateLatest(USER_CODE);

        assertThat(result.overallTrend()).isEqualTo(SkinStateTrend.STABLE);
    }

    @Test
    void reportsUnknownWhenFewerThanTwoSnapshots() {
        SkinStateSnapshot only = snap(LocalDate.of(2026, 8, 17), VERSION, scores(0, 0, 0, 1, 0, 0, 0, 0));
        when(snapshotRepository.findTop2ByUser_UserCodeOrderBySnapshotDateDesc(USER_CODE))
                .thenReturn(List.of(only));

        TrendResult result = service.evaluateLatest(USER_CODE);

        assertThat(result.overallTrend()).isEqualTo(SkinStateTrend.UNKNOWN);
        assertThat(result.toRuleSignals()).containsEntry("trend_unknown", true);
    }

    @Test
    void reportsUnknownWhenScoringVersionsDiffer() {
        SkinStateSnapshot latest = snap(LocalDate.of(2026, 8, 17), "selfcheck-ordinal-v2", scores(0, 0, 0, 1, 0, 0, 0, 0));
        SkinStateSnapshot previous = snap(LocalDate.of(2026, 8, 16), VERSION, scores(0, 0, 0, 3, 0, 0, 0, 0));
        when(snapshotRepository.findTop2ByUser_UserCodeOrderBySnapshotDateDesc(USER_CODE))
                .thenReturn(List.of(latest, previous));

        TrendResult result = service.evaluateLatest(USER_CODE);

        assertThat(result.overallTrend()).isEqualTo(SkinStateTrend.UNKNOWN);
    }

    @Test
    void throwsUserNotFoundForUnknownUser() {
        when(userRepository.existsByUserCode(USER_CODE)).thenReturn(false);

        assertThatThrownBy(() -> service.evaluateLatest(USER_CODE))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.errorCode()).isEqualTo(ErrorCode.USER_NOT_FOUND));
    }
}
