package com.innerderma.carehistory.application;

import com.innerderma.carecycle.domain.CareCycle;
import com.innerderma.carecycle.domain.CareCycleRepository;
import com.innerderma.caresolution.domain.*;
import com.innerderma.common.error.BusinessException;
import com.innerderma.skinanalysis.domain.SkinAnalysis;
import com.innerderma.skinanalysis.domain.SkinAnalysisRepository;
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
    private SkinCaptureRepository captureRepository;
    private SkinAnalysisRepository analysisRepository;
    private CareCycleRepository cycleRepository;
    private CareSolutionRepository solutionRepository;
    private CareHistoryService service;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        captureRepository = mock(SkinCaptureRepository.class);
        analysisRepository = mock(SkinAnalysisRepository.class);
        cycleRepository = mock(CareCycleRepository.class);
        solutionRepository = mock(CareSolutionRepository.class);
        service = new CareHistoryService(userRepository, captureRepository, analysisRepository,
                cycleRepository, solutionRepository);
        when(userRepository.existsByUserCode(USER_CODE)).thenReturn(true);
    }

    @Test
    void returnsCompletedCareRecordsInRequestedRange() {
        LocalDate from = LocalDate.of(2026, 8, 1);
        LocalDate to = LocalDate.of(2026, 8, 17);
        CareSolution solution = solution(to);
        SkinCapture capture = solution.getCareCycle().getSkinAnalysis().getSkinCapture();
        when(captureRepository.findByUser_UserCodeAndCapturedDateBetweenAndQualityStatusOrderByCapturedDateDescCapturedAtDesc(
                USER_CODE, from, to, SkinCaptureQualityStatus.VALID)).thenReturn(List.of(capture));
        when(analysisRepository.findBySkinCapture_Id(capture.getId()))
                .thenReturn(java.util.Optional.of(solution.getCareCycle().getSkinAnalysis()));
        when(cycleRepository.findBySkinAnalysis_Id(solution.getCareCycle().getSkinAnalysis().getId()))
                .thenReturn(java.util.Optional.of(solution.getCareCycle()));
        when(solutionRepository.findByCareCycle_Id(solution.getCareCycle().getId()))
                .thenReturn(java.util.Optional.of(solution));

        CareHistoryResult result = service.getHistory(USER_CODE, from, to);

        assertThat(result.from()).isEqualTo(from);
        assertThat(result.to()).isEqualTo(to);
        assertThat(result.items()).singleElement().satisfies(item -> {
            assertThat(item.date()).isEqualTo(to);
            assertThat(item.headline()).isEqualTo("오늘의 케어");
            assertThat(item.safetyLevel()).isEqualTo(SafetyLevel.NORMAL);
            assertThat(item.progressStatus()).isEqualTo(CareProgressStatus.SOLUTION_READY);
        });
    }

    @Test
    void rejectsRangesLongerThanThirtyOneDays() {
        assertThatThrownBy(() -> service.getHistory(USER_CODE,
                LocalDate.of(2026, 7, 1), LocalDate.of(2026, 8, 17)))
                .isInstanceOf(BusinessException.class);
        verifyNoInteractions(captureRepository, analysisRepository, cycleRepository, solutionRepository);
    }

    @Test
    void includesCaptureEvenWhenLaterProcessingIsNotFinished() {
        LocalDate date = LocalDate.of(2026, 8, 17);
        User user = new User(USER_CODE, "테스트 사용자", "010-1234-1234");
        SkinCapture capture = new SkinCapture(user, date, date.atTime(9, 0), "/face.jpg",
                "face.jpg", "image/jpeg", 3, SkinCaptureQualityStatus.VALID);
        when(captureRepository.findByUser_UserCodeAndCapturedDateBetweenAndQualityStatusOrderByCapturedDateDescCapturedAtDesc(
                USER_CODE, date, date, SkinCaptureQualityStatus.VALID)).thenReturn(List.of(capture));
        when(analysisRepository.findBySkinCapture_Id(capture.getId()))
                .thenReturn(java.util.Optional.empty());

        CareHistoryResult result = service.getHistory(USER_CODE, date, date);

        assertThat(result.items()).singleElement().satisfies(item -> {
            assertThat(item.progressStatus()).isEqualTo(CareProgressStatus.CAPTURED);
            assertThat(item.analysisId()).isNull();
            assertThat(item.careSolutionId()).isNull();
        });
    }

    @Test
    void reportsNotFoundWhenDateHasNoValidCapture() {
        LocalDate date = LocalDate.of(2026, 8, 17);
        when(captureRepository.findByUser_UserCodeAndCapturedDateBetweenAndQualityStatusOrderByCapturedDateDescCapturedAtDesc(
                USER_CODE, date, date, SkinCaptureQualityStatus.VALID)).thenReturn(List.of());

        assertThatThrownBy(() -> service.getDailyDetail(USER_CODE, date))
                .isInstanceOf(BusinessException.class)
                .satisfies(error -> assertThat(((BusinessException) error).errorCode())
                        .isEqualTo(com.innerderma.common.error.ErrorCode.CARE_HISTORY_NOT_FOUND));
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
