package com.innerderma.skindiagnosis.application;

import com.innerderma.common.error.BusinessException;
import com.innerderma.common.error.ErrorCode;
import com.innerderma.skindiagnosis.domain.WhsSkinDiagnosis;
import com.innerderma.skindiagnosis.domain.WhsSkinDiagnosisRepository;
import com.innerderma.user.domain.User;
import com.innerderma.user.domain.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class WhsSkinDiagnosisServiceTest {

    private static final String USER_CODE = "WHS-DEMO-001";
    private static final Clock CLOCK = Clock.fixed(
            Instant.parse("2026-08-17T03:30:00Z"), ZoneId.of("Asia/Seoul")
    );

    private WhsSkinDiagnosisRepository diagnosisRepository;
    private UserRepository userRepository;
    private WhsSkinDiagnosisService service;

    @BeforeEach
    void setUp() {
        diagnosisRepository = mock(WhsSkinDiagnosisRepository.class);
        userRepository = mock(UserRepository.class);
        service = new WhsSkinDiagnosisService(diagnosisRepository, userRepository, CLOCK);
    }

    @Test
    void historyDefaultsToRecentThirtyDaysUsingKoreanToday() {
        User user = new User(USER_CODE, "테스트 사용자", "010-1234-1234");
        WhsSkinDiagnosis diagnosis = new WhsSkinDiagnosis(user, LocalDate.of(2026, 8, 10), "건성 경향");
        when(userRepository.existsByUserCode(USER_CODE)).thenReturn(true);
        when(diagnosisRepository.findByUser_UserCodeAndDiagnosedDateBetweenOrderByDiagnosedDateDesc(
                any(), any(), any())).thenReturn(List.of(diagnosis));

        var result = service.getHistory(USER_CODE, null, null);

        // CLOCK은 Asia/Seoul 기준 2026-08-17이며 기본 범위는 최근 30일(7/19~8/17)이다.
        assertThat(result.to()).isEqualTo(LocalDate.of(2026, 8, 17));
        assertThat(result.from()).isEqualTo(LocalDate.of(2026, 7, 19));
        assertThat(result.items()).hasSize(1);
    }

    @Test
    void historyRejectsRangeLongerThanThirtyOneDays() {
        when(userRepository.existsByUserCode(USER_CODE)).thenReturn(true);

        assertThatThrownBy(() -> service.getHistory(USER_CODE,
                LocalDate.of(2026, 7, 1), LocalDate.of(2026, 8, 17)))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.errorCode()).isEqualTo(ErrorCode.INVALID_REQUEST)
                );
        verify(diagnosisRepository, never())
                .findByUser_UserCodeAndDiagnosedDateBetweenOrderByDiagnosedDateDesc(any(), any(), any());
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

    @Test
    void historyRejectsUnknownUser() {
        when(userRepository.existsByUserCode(USER_CODE)).thenReturn(false);

        assertThatThrownBy(() -> service.getHistory(USER_CODE, null, null))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.errorCode()).isEqualTo(ErrorCode.USER_NOT_FOUND)
                );
    }
}
