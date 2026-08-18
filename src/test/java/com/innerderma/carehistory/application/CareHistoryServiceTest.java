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

    @Test
    void sameDateCanContainPreviousMorningAndNewEveningCare() {
        LocalDate servedDate = LocalDate.of(2026, 8, 18);
        SkinCapture previousCapture = mock(SkinCapture.class);
        SkinCapture newCapture = mock(SkinCapture.class);
        when(previousCapture.getId()).thenReturn(1L);
        when(previousCapture.getCapturedDate()).thenReturn(servedDate.minusDays(1));
        when(newCapture.getId()).thenReturn(2L);
        when(newCapture.getCapturedDate()).thenReturn(servedDate);
        SkinAnalysis previousAnalysis = mock(SkinAnalysis.class);
        SkinAnalysis newAnalysis = mock(SkinAnalysis.class);
        when(previousAnalysis.getId()).thenReturn(11L);
        when(newAnalysis.getId()).thenReturn(12L);
        CareCycle previousCycle = mock(CareCycle.class);
        CareCycle newCycle = mock(CareCycle.class);
        when(previousCycle.getId()).thenReturn(21L);
        when(previousCycle.getEveningCareDate()).thenReturn(servedDate.minusDays(1));
        when(previousCycle.getMorningCareDate()).thenReturn(servedDate);
        when(newCycle.getId()).thenReturn(22L);
        when(newCycle.getEveningCareDate()).thenReturn(servedDate);
        when(newCycle.getMorningCareDate()).thenReturn(servedDate.plusDays(1));
        when(previousCycle.getOriginCaptureDate()).thenReturn(servedDate.minusDays(1));
        when(newCycle.getOriginCaptureDate()).thenReturn(servedDate);
        when(previousCycle.getSkinAnalysis()).thenReturn(previousAnalysis);
        when(newCycle.getSkinAnalysis()).thenReturn(newAnalysis);
        when(previousAnalysis.getSkinCapture()).thenReturn(previousCapture);
        when(newAnalysis.getSkinCapture()).thenReturn(newCapture);
        CareSolution previousSolution = mock(CareSolution.class);
        CareSolution newSolution = mock(CareSolution.class);
        when(previousSolution.getCareCycle()).thenReturn(previousCycle);
        when(newSolution.getCareCycle()).thenReturn(newCycle);

        when(captureRepository.findByUser_UserCodeAndCapturedDateBetweenAndQualityStatusOrderByCapturedDateDescCapturedAtDesc(
                USER_CODE, servedDate.minusDays(1), servedDate, SkinCaptureQualityStatus.VALID))
                .thenReturn(List.of(newCapture, previousCapture));
        when(analysisRepository.findBySkinCapture_Id(1L)).thenReturn(java.util.Optional.of(previousAnalysis));
        when(analysisRepository.findBySkinCapture_Id(2L)).thenReturn(java.util.Optional.of(newAnalysis));
        when(cycleRepository.findBySkinAnalysis_Id(11L)).thenReturn(java.util.Optional.of(previousCycle));
        when(cycleRepository.findBySkinAnalysis_Id(12L)).thenReturn(java.util.Optional.of(newCycle));
        when(solutionRepository
                .findFirstByCareCycle_User_UserCodeAndCareCycle_OriginCaptureDateLessThanEqualOrderByCareCycle_OriginCaptureDateDescGeneratedAtDesc(
                        USER_CODE, servedDate.minusDays(1))).thenReturn(java.util.Optional.of(previousSolution));
        when(solutionRepository
                .findFirstByCareCycle_User_UserCodeAndCareCycle_OriginCaptureDateLessThanEqualOrderByCareCycle_OriginCaptureDateDescGeneratedAtDesc(
                        USER_CODE, servedDate)).thenReturn(java.util.Optional.of(newSolution));

        DailyCareHistoryResult result = service.getDailyDetail(USER_CODE, servedDate);

        assertThat(result.items()).extracting(DailyCareHistoryItem::phase)
                .containsExactly(CarePhase.MORNING, CarePhase.EVENING);
        assertThat(result.items().get(0).inherited()).isTrue();
        assertThat(result.items().get(0).generationType()).isEqualTo(CareGenerationType.CARRIED_FORWARD);
        assertThat(result.items().get(0).history().careCycleId()).isEqualTo(21L);
        assertThat(result.items().get(1).inherited()).isFalse();
        assertThat(result.items().get(1).generationType()).isEqualTo(CareGenerationType.NEW_ANALYSIS);
        assertThat(result.items().get(1).history().careCycleId()).isEqualTo(22L);
    }

    @Test
    void noPhotoDayInheritsLatestSolutionForBothPhases() {
        LocalDate originDate = LocalDate.of(2026, 8, 17);
        LocalDate servedDate = originDate.plusDays(3);
        CareSolution solution = solution(originDate);
        SkinCapture capture = solution.getCareCycle().getSkinAnalysis().getSkinCapture();
        when(solutionRepository
                .findFirstByCareCycle_User_UserCodeAndCareCycle_OriginCaptureDateLessThanEqualOrderByCareCycle_OriginCaptureDateDescGeneratedAtDesc(
                        eq(USER_CODE), any(LocalDate.class))).thenReturn(java.util.Optional.of(solution));
        when(analysisRepository.findBySkinCapture_Id(capture.getId()))
                .thenReturn(java.util.Optional.of(solution.getCareCycle().getSkinAnalysis()));
        when(cycleRepository.findBySkinAnalysis_Id(solution.getCareCycle().getSkinAnalysis().getId()))
                .thenReturn(java.util.Optional.of(solution.getCareCycle()));
        when(solutionRepository.findByCareCycle_Id(solution.getCareCycle().getId()))
                .thenReturn(java.util.Optional.of(solution));

        DailyCareHistoryResult result = service.getDailyDetail(USER_CODE, servedDate);

        assertThat(result.items()).extracting(DailyCareHistoryItem::phase)
                .containsExactly(CarePhase.MORNING, CarePhase.EVENING);
        assertThat(result.items()).allMatch(DailyCareHistoryItem::inherited);
        assertThat(result.items())
                .allMatch(item -> item.generationType() == CareGenerationType.CARRIED_FORWARD);
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
