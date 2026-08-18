package com.innerderma.selfcheck.application;

import com.innerderma.common.error.BusinessException;
import com.innerderma.common.error.ErrorCode;
import com.innerderma.selfcheck.domain.SelfCheck;
import com.innerderma.selfcheck.domain.SelfCheckRepository;
import com.innerderma.selfcheck.domain.SymptomSeverity;
import com.innerderma.user.domain.User;
import com.innerderma.user.domain.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SelfCheckServiceTest {

    private static final String USER_CODE = "WHS-DEMO-001";
    private static final Clock CLOCK = Clock.fixed(
            Instant.parse("2026-08-17T03:30:00Z"),
            ZoneId.of("Asia/Seoul")
    );

    private SelfCheckRepository selfCheckRepository;
    private UserRepository userRepository;
    private SelfCheckService service;

    @BeforeEach
    void setUp() {
        selfCheckRepository = mock(SelfCheckRepository.class);
        userRepository = mock(UserRepository.class);
        service = new SelfCheckService(selfCheckRepository, userRepository, CLOCK);
    }

    @Test
    void createsSelfCheckUsingKoreanLocalTimeAndNormalizesNote() {
        User user = new User(USER_CODE, "테스트 사용자", "010-1234-1234");
        when(userRepository.findByUserCode(USER_CODE)).thenReturn(Optional.of(user));
        when(selfCheckRepository.save(any(SelfCheck.class))).thenAnswer(invocation -> invocation.getArgument(0));

        SelfCheck result = service.create(USER_CODE, command(SymptomSeverity.MILD, "  건조함이 느껴짐  "));

        assertThat(result.getCheckedAt()).hasToString("2026-08-17T12:30");
        assertThat(result.getDryness()).isEqualTo(SymptomSeverity.MILD);
        assertThat(result.getNote()).isEqualTo("건조함이 느껴짐");
        assertThat(result.requiresSafetyAttention()).isFalse();
    }

    @Test
    void throwsUserNotFoundWhenCreatingForUnknownUser() {
        when(userRepository.findByUserCode("UNKNOWN")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create("UNKNOWN", command(SymptomSeverity.NONE, null)))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.errorCode()).isEqualTo(ErrorCode.USER_NOT_FOUND)
                );
    }

    @Test
    void throwsSelfCheckNotFoundWhenUserHasNoRecords() {
        when(userRepository.existsByUserCode(USER_CODE)).thenReturn(true);
        when(selfCheckRepository.findFirstByUser_UserCodeOrderByCheckedAtDesc(USER_CODE))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getLatest(USER_CODE))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.errorCode()).isEqualTo(ErrorCode.SELF_CHECK_NOT_FOUND)
                );
    }

    @Test
    void historyDefaultsToRecentThirtyDaysUsingKoreanToday() {
        when(userRepository.existsByUserCode(USER_CODE)).thenReturn(true);
        when(selfCheckRepository.findByUser_UserCodeAndCheckedAtBetweenOrderByCheckedAtDesc(
                any(), any(), any())).thenReturn(java.util.List.of());

        var result = service.getHistory(USER_CODE, null, null);

        // CLOCK은 Asia/Seoul 기준 2026-08-17이며 기본 범위는 최근 30일(7/19~8/17)이다.
        assertThat(result.to()).isEqualTo(LocalDate.of(2026, 8, 17));
        assertThat(result.from()).isEqualTo(LocalDate.of(2026, 7, 19));
        assertThat(result.items()).isEmpty();
    }

    @Test
    void historyRejectsRangeLongerThanThirtyOneDays() {
        when(userRepository.existsByUserCode(USER_CODE)).thenReturn(true);

        assertThatThrownBy(() -> service.getHistory(USER_CODE,
                LocalDate.of(2026, 7, 1), LocalDate.of(2026, 8, 17)))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.errorCode()).isEqualTo(ErrorCode.INVALID_REQUEST)
                );
        verify(selfCheckRepository, never())
                .findByUser_UserCodeAndCheckedAtBetweenOrderByCheckedAtDesc(any(), any(), any());
    }

    @Test
    void historyRejectsReversedRange() {
        when(userRepository.existsByUserCode(USER_CODE)).thenReturn(true);

        assertThatThrownBy(() -> service.getHistory(USER_CODE,
                LocalDate.of(2026, 8, 17), LocalDate.of(2026, 8, 1)))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.errorCode()).isEqualTo(ErrorCode.INVALID_REQUEST)
                );
    }

    private SelfCheckCommand command(SymptomSeverity dryness, String note) {
        return new SelfCheckCommand(
                SymptomSeverity.NONE,
                SymptomSeverity.NONE,
                SymptomSeverity.NONE,
                dryness,
                SymptomSeverity.NONE,
                SymptomSeverity.NONE,
                SymptomSeverity.NONE,
                SymptomSeverity.NONE,
                note
        );
    }
}
