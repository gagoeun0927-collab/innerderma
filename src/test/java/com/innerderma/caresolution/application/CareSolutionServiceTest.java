package com.innerderma.caresolution.application;

import com.innerderma.carecycle.domain.CareCycle;
import com.innerderma.carecycle.domain.CareCycleRepository;
import com.innerderma.caresolution.domain.*;
import com.innerderma.facility.domain.Facility;
import com.innerderma.procedure.domain.ProcedureRecord;
import com.innerderma.procedure.domain.ProcedureRecordRepository;
import com.innerderma.selfcheck.domain.SelfCheck;
import com.innerderma.selfcheck.domain.SymptomSeverity;
import com.innerderma.skinanalysis.application.SkinAgeAnalysisResult;
import com.innerderma.skinanalysis.domain.SkinAnalysis;
import com.innerderma.skincapture.domain.SkinCapture;
import com.innerderma.skincapture.domain.SkinCaptureQualityStatus;
import com.innerderma.skindiagnosis.domain.WhsSkinDiagnosisRepository;
import com.innerderma.user.domain.User;
import com.innerderma.user.domain.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import java.time.*;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CareSolutionServiceTest {
    private static final String USER_CODE = "WHS-DEMO-001";
    private static final Clock CLOCK = Clock.fixed(
            Instant.parse("2026-08-17T03:30:00Z"), ZoneId.of("Asia/Seoul"));

    private CareSolutionRepository solutionRepository;
    private CareCycleRepository cycleRepository;
    private WhsSkinDiagnosisRepository diagnosisRepository;
    private ProcedureRecordRepository procedureRepository;
    private UserRepository userRepository;
    private ObjectMapper objectMapper;
    private CareSolutionService service;
    private User user;

    @BeforeEach
    void setUp() {
        solutionRepository = mock(CareSolutionRepository.class);
        cycleRepository = mock(CareCycleRepository.class);
        diagnosisRepository = mock(WhsSkinDiagnosisRepository.class);
        procedureRepository = mock(ProcedureRecordRepository.class);
        userRepository = mock(UserRepository.class);
        objectMapper = new ObjectMapper();
        service = new CareSolutionService(solutionRepository, cycleRepository, diagnosisRepository,
                procedureRepository, userRepository, objectMapper, CLOCK);
        user = new User(USER_CODE, "테스트 사용자", "010-1234-1234");
        when(userRepository.existsByUserCode(USER_CODE)).thenReturn(true);
        when(solutionRepository.save(any(CareSolution.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void prioritizesSafetyAttentionAndProcedureGuide() {
        CareCycle cycle = cycle(true, LocalDate.of(2026, 8, 17));
        ProcedureRecord procedure = new ProcedureRecord(user, new Facility("DERNA", "더나클리닉"),
                LocalDate.of(2026, 8, 15), "진정 및 피부 장벽 관리", "자극적인 제품을 피하세요.");
        when(cycleRepository.findFirstByUser_UserCodeAndOriginCaptureDateLessThanEqualOrderByOriginCaptureDateDescCreatedAtDesc(
                USER_CODE, LocalDate.of(2026, 8, 17))).thenReturn(Optional.of(cycle));
        when(procedureRepository.findFirstByUser_UserCodeAndProcedureDateLessThanEqualOrderByProcedureDateDesc(
                USER_CODE, LocalDate.of(2026, 8, 17))).thenReturn(Optional.of(procedure));

        CareSolutionResult result = service.create(USER_CODE, null);

        assertThat(result.solution().getSafetyLevel()).isEqualTo(SafetyLevel.ATTENTION);
        assertThat(result.solution().getSeason()).isEqualTo(CareSeason.SUMMER);
        assertThat(result.solution().getSafetyMessage()).contains("시술기관 또는 의료진");
        assertThat(result.eveningSteps()).anyMatch(step -> step.contains("레티놀"));
        assertThat(result.eveningSteps()).anyMatch(step -> step.contains("자극적인 제품을 피하세요"));
    }

    @Test
    void usesLowestHealthyScoreAsPrimaryConcern() {
        CareCycle cycle = cycle(false, LocalDate.of(2026, 8, 17));
        when(cycleRepository.findFirstByUser_UserCodeAndOriginCaptureDateLessThanEqualOrderByOriginCaptureDateDescCreatedAtDesc(
                USER_CODE, LocalDate.of(2026, 8, 17))).thenReturn(Optional.of(cycle));

        CareSolutionResult result = service.create(USER_CODE, null);

        assertThat(result.solution().getSafetyLevel()).isEqualTo(SafetyLevel.NORMAL);
        assertThat(result.solution().getPrimaryConcern()).isEqualTo("redness");
        assertThat(result.solution().getHeadline()).contains("홍조", "가벼운 보습");
        assertThat(result.morningSteps()).anyMatch(step -> step.contains("자외선 차단제"));
    }

    @Test
    void returnsStoredSolutionAsInheritedWithoutRegenerating() {
        CareCycle cycle = cycle(false, LocalDate.of(2026, 8, 17));
        CareSolution stored = new CareSolution(cycle, null, null, CareSeason.SUMMER,
                SafetyLevel.NORMAL, "관리 안내", "[\"저녁 보습\"]", "[\"아침 자외선 차단\"]",
                null, "redness", LocalDateTime.of(2026, 8, 17, 12, 30));
        when(solutionRepository.findFirstByCareCycle_User_UserCodeAndCareCycle_OriginCaptureDateLessThanEqualOrderByCareCycle_OriginCaptureDateDescGeneratedAtDesc(
                USER_CODE, LocalDate.of(2026, 8, 19))).thenReturn(Optional.of(stored));

        CareSolutionResult result = service.getDaily(USER_CODE, LocalDate.of(2026, 8, 19));

        assertThat(result.inherited()).isTrue();
        assertThat(result.eveningSteps()).containsExactly("저녁 보습");
        assertThat(result.morningSteps()).containsExactly("아침 자외선 차단");
    }

    private CareCycle cycle(boolean attention, LocalDate date) {
        SkinCapture capture = new SkinCapture(user, date, date.atTime(10, 0), "/images/face.jpg",
                "face.jpg", "image/jpeg", 3, SkinCaptureQualityStatus.VALID);
        SkinAnalysis analysis = new SkinAnalysis(capture, date.atTime(10, 1), 70, "Good", "1.0",
                objectMapper.writeValueAsString(validAnalysis()));
        SelfCheck selfCheck = new SelfCheck(user, date.atTime(10, 2),
                attention ? SymptomSeverity.MODERATE : SymptomSeverity.NONE,
                SymptomSeverity.NONE, SymptomSeverity.MILD, SymptomSeverity.MILD,
                SymptomSeverity.NONE, SymptomSeverity.NONE, SymptomSeverity.NONE,
                SymptomSeverity.NONE, SymptomSeverity.NONE, SymptomSeverity.NONE,
                SymptomSeverity.NONE, null);
        return new CareCycle(user, analysis, selfCheck, date, date.atTime(10, 3));
    }

    private SkinAgeAnalysisResult validAnalysis() {
        return new SkinAgeAnalysisResult(
                new SkinAgeAnalysisResult.Summary(24, 24, 0.0, 70, "Good"), List.of(),
                new SkinAgeAnalysisResult.AggregateMetrics(70, 70,
                        Map.of("wrinkle", 80.0, "pore_texture", 65.0,
                                "pigmentation", 70.0, "redness", 40.0), List.of()),
                null, new SkinAgeAnalysisResult.Metadata(10, "1.0", "cpu", 512));
    }
}
