package com.innerderma.skinstate.application;

import com.innerderma.common.error.BusinessException;
import com.innerderma.common.error.ErrorCode;
import com.innerderma.selfcheck.domain.SelfCheck;
import com.innerderma.selfcheck.domain.SelfCheckRepository;
import com.innerderma.selfcheck.domain.SymptomSeverity;
import com.innerderma.skinstate.domain.SkinStateSnapshot;
import com.innerderma.skinstate.domain.SkinStateSnapshotRepository;
import com.innerderma.user.domain.User;
import com.innerderma.user.domain.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SkinStateSnapshotServiceTest {

    private static final String USER_CODE = "WHS-DEMO-001";
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-08-17T03:30:00Z"), ZoneId.of("Asia/Seoul"));

    private SkinStateSnapshotRepository snapshotRepository;
    private SelfCheckRepository selfCheckRepository;
    private UserRepository userRepository;
    private SkinStateSnapshotService service;

    @BeforeEach
    void setUp() {
        snapshotRepository = mock(SkinStateSnapshotRepository.class);
        selfCheckRepository = mock(SelfCheckRepository.class);
        userRepository = mock(UserRepository.class);
        service = new SkinStateSnapshotService(
                snapshotRepository, selfCheckRepository, mock(com.innerderma.skinanalysis.domain.SkinAnalysisRepository.class), userRepository, new ObjectMapper(), mock(com.innerderma.airule.cache.SolutionCache.class), CLOCK);
    }

    private User user() {
        return new User(USER_CODE, "테스트 사용자", "010-1234-1234");
    }

    private SelfCheck selfCheck(SymptomSeverity pain, SymptomSeverity heat, SymptomSeverity tightness,
                                SymptomSeverity dryness, SymptomSeverity itching, SymptomSeverity swelling,
                                SymptomSeverity peeling, SymptomSeverity breakout) {
        return new SelfCheck(user(), LocalDateTime.of(2026, 8, 17, 12, 30),
                pain, heat, tightness, dryness, itching, swelling, peeling, breakout,
                SymptomSeverity.NONE, SymptomSeverity.NONE, SymptomSeverity.NONE, null);
    }

    @Test
    void createsSnapshotWithOrdinalScoresAndScoringVersion() {
        when(userRepository.findByUserCode(USER_CODE)).thenReturn(Optional.of(user()));
        when(selfCheckRepository.findFirstByUser_UserCodeOrderByCheckedAtDesc(USER_CODE))
                .thenReturn(Optional.of(selfCheck(
                        SymptomSeverity.NONE, SymptomSeverity.MILD, SymptomSeverity.NONE, SymptomSeverity.SEVERE,
                        SymptomSeverity.NONE, SymptomSeverity.NONE, SymptomSeverity.NONE, SymptomSeverity.MODERATE)));
        when(snapshotRepository.findByUser_UserCodeAndSnapshotDate(USER_CODE, LocalDate.of(2026, 8, 17)))
                .thenReturn(Optional.empty());
        when(snapshotRepository.save(any(SkinStateSnapshot.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        SkinStateSnapshotResult result = service.refreshFromLatestSelfCheck(USER_CODE);

        assertThat(result.snapshot().getScoringVersion()).isEqualTo(SkinStateSnapshotService.SCORING_VERSION);
        assertThat(result.snapshot().getSnapshotDate()).isEqualTo(LocalDate.of(2026, 8, 17));
        assertThat(result.symptomScores())
                .containsEntry("heatSensation", 1)
                .containsEntry("dryness", 3)
                .containsEntry("breakout", 2)
                .containsEntry("pain", 0);
        // dryness(3)가 최고점이므로 dominant = dryness
        assertThat(result.snapshot().getDominantSymptom()).isEqualTo("dryness");
    }

    @Test
    void breaksTiesUsingFixedAxisOrder() {
        when(userRepository.findByUserCode(USER_CODE)).thenReturn(Optional.of(user()));
        when(selfCheckRepository.findFirstByUser_UserCodeOrderByCheckedAtDesc(USER_CODE))
                .thenReturn(Optional.of(selfCheck(
                        SymptomSeverity.NONE, SymptomSeverity.MILD, SymptomSeverity.NONE, SymptomSeverity.MODERATE,
                        SymptomSeverity.MODERATE, SymptomSeverity.NONE, SymptomSeverity.NONE, SymptomSeverity.NONE)));
        when(snapshotRepository.findByUser_UserCodeAndSnapshotDate(any(), any())).thenReturn(Optional.empty());
        when(snapshotRepository.save(any(SkinStateSnapshot.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        SkinStateSnapshotResult result = service.refreshFromLatestSelfCheck(USER_CODE);

        // dryness 와 itching 이 동점(2)이지만 AXIS_ORDER 에서 dryness 가 먼저이므로 dominant = dryness
        assertThat(result.snapshot().getDominantSymptom()).isEqualTo("dryness");
    }

    @Test
    void hasNoDominantSymptomWhenAllScoresAreZero() {
        when(userRepository.findByUserCode(USER_CODE)).thenReturn(Optional.of(user()));
        when(selfCheckRepository.findFirstByUser_UserCodeOrderByCheckedAtDesc(USER_CODE))
                .thenReturn(Optional.of(selfCheck(
                        SymptomSeverity.NONE, SymptomSeverity.NONE, SymptomSeverity.NONE, SymptomSeverity.NONE,
                        SymptomSeverity.NONE, SymptomSeverity.NONE, SymptomSeverity.NONE, SymptomSeverity.NONE)));
        when(snapshotRepository.findByUser_UserCodeAndSnapshotDate(any(), any())).thenReturn(Optional.empty());
        when(snapshotRepository.save(any(SkinStateSnapshot.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        SkinStateSnapshotResult result = service.refreshFromLatestSelfCheck(USER_CODE);

        assertThat(result.snapshot().getDominantSymptom()).isNull();
    }

    @Test
    void updatesExistingSnapshotInPlaceOnSameDate() {
        SkinStateSnapshot existing = new SkinStateSnapshot(user(), LocalDate.of(2026, 8, 17),
                "old-version", "{}", null, "pain", 1L, null, LocalDateTime.of(2026, 8, 17, 9, 0));
        when(userRepository.findByUserCode(USER_CODE)).thenReturn(Optional.of(user()));
        when(selfCheckRepository.findFirstByUser_UserCodeOrderByCheckedAtDesc(USER_CODE))
                .thenReturn(Optional.of(selfCheck(
                        SymptomSeverity.NONE, SymptomSeverity.NONE, SymptomSeverity.NONE, SymptomSeverity.SEVERE,
                        SymptomSeverity.NONE, SymptomSeverity.NONE, SymptomSeverity.NONE, SymptomSeverity.NONE)));
        when(snapshotRepository.findByUser_UserCodeAndSnapshotDate(USER_CODE, LocalDate.of(2026, 8, 17)))
                .thenReturn(Optional.of(existing));

        SkinStateSnapshotResult result = service.refreshFromLatestSelfCheck(USER_CODE);

        assertThat(result.snapshot()).isSameAs(existing);
        assertThat(existing.getScoringVersion()).isEqualTo(SkinStateSnapshotService.SCORING_VERSION);
        assertThat(existing.getDominantSymptom()).isEqualTo("dryness");
        verify(snapshotRepository, never()).save(any());
    }

    @Test
    void throwsSelfCheckNotFoundWhenUserHasNoSelfCheck() {
        when(userRepository.findByUserCode(USER_CODE)).thenReturn(Optional.of(user()));
        when(selfCheckRepository.findFirstByUser_UserCodeOrderByCheckedAtDesc(USER_CODE))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.refreshFromLatestSelfCheck(USER_CODE))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.errorCode()).isEqualTo(ErrorCode.SELF_CHECK_NOT_FOUND));
    }

    @Test
    void throwsSnapshotNotFoundWhenNoSnapshotExists() {
        when(userRepository.existsByUserCode(USER_CODE)).thenReturn(true);
        when(snapshotRepository.findFirstByUser_UserCodeOrderBySnapshotDateDesc(USER_CODE))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getLatest(USER_CODE))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.errorCode()).isEqualTo(ErrorCode.SKIN_STATE_SNAPSHOT_NOT_FOUND));
    }

    @Test
    void throwsUserNotFoundOnGetLatestForUnknownUser() {
        when(userRepository.existsByUserCode(USER_CODE)).thenReturn(false);

        assertThatThrownBy(() -> service.getLatest(USER_CODE))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.errorCode()).isEqualTo(ErrorCode.USER_NOT_FOUND));
    }
}
