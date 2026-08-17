package com.innerderma.carehistory.application;

import com.innerderma.carecycle.domain.CareCycle;
import com.innerderma.caresolution.domain.*;
import com.innerderma.common.error.BusinessException;
import com.innerderma.skinanalysis.domain.SkinAnalysis;
import com.innerderma.skincapture.domain.*;
import com.innerderma.user.domain.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class CareHistoryServiceTest {
    private static final String USER_CODE = "WHS-DEMO-001";
    private UserRepository userRepository;
    private CareSolutionRepository solutionRepository;
    private CareHistoryService service;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        solutionRepository = mock(CareSolutionRepository.class);
        service = new CareHistoryService(userRepository, solutionRepository);
        when(userRepository.existsByUserCode(USER_CODE)).thenReturn(true);
    }

    @Test
    void returnsCompletedCareRecordsInRequestedRange() {
        LocalDate from = LocalDate.of(2026, 8, 1);
        LocalDate to = LocalDate.of(2026, 8, 17);
        when(solutionRepository
                .findByCareCycle_User_UserCodeAndCareCycle_OriginCaptureDateBetweenOrderByCareCycle_OriginCaptureDateDesc(
                        USER_CODE, from, to)).thenReturn(List.of(solution(to)));

        CareHistoryResult result = service.getHistory(USER_CODE, from, to);

        assertThat(result.from()).isEqualTo(from);
        assertThat(result.to()).isEqualTo(to);
        assertThat(result.items()).singleElement().satisfies(item -> {
            assertThat(item.date()).isEqualTo(to);
            assertThat(item.headline()).isEqualTo("오늘의 케어");
            assertThat(item.safetyLevel()).isEqualTo(SafetyLevel.NORMAL);
        });
    }

    @Test
    void rejectsRangesLongerThanThirtyOneDays() {
        assertThatThrownBy(() -> service.getHistory(USER_CODE,
                LocalDate.of(2026, 7, 1), LocalDate.of(2026, 8, 17)))
                .isInstanceOf(BusinessException.class);
        verifyNoInteractions(solutionRepository);
    }

    private CareSolution solution(LocalDate date) {
        User user = new User(USER_CODE, "테스트 사용자", "010-1234-1234");
        SkinCapture capture = new SkinCapture(user, date, date.atTime(9, 0), "/face.jpg",
                "face.jpg", "image/jpeg", 3, SkinCaptureQualityStatus.VALID);
        SkinAnalysis analysis = new SkinAnalysis(capture, date.atTime(9, 1), 80,
                "Good", "1.0", "{}");
        CareCycle cycle = new CareCycle(user, analysis, null, date, date.atTime(9, 2));
        return new CareSolution(cycle, null, null, CareSeason.SUMMER, SafetyLevel.NORMAL,
                "오늘의 케어", "[]", "[]", null, "redness", date.atTime(9, 3));
    }
}
