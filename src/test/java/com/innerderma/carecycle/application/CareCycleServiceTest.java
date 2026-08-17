package com.innerderma.carecycle.application;

import com.innerderma.carecycle.domain.CareCycle;
import com.innerderma.carecycle.domain.CareCycleRepository;
import com.innerderma.common.error.BusinessException;
import com.innerderma.common.error.ErrorCode;
import com.innerderma.selfcheck.domain.SelfCheckRepository;
import com.innerderma.skinanalysis.domain.SkinAnalysis;
import com.innerderma.skinanalysis.domain.SkinAnalysisRepository;
import com.innerderma.skincapture.domain.SkinCapture;
import com.innerderma.skincapture.domain.SkinCaptureQualityStatus;
import com.innerderma.user.domain.User;
import com.innerderma.user.domain.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.*;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class CareCycleServiceTest {
    private static final String USER_CODE = "WHS-DEMO-001";
    private static final Clock CLOCK = Clock.fixed(
            Instant.parse("2026-08-17T03:30:00Z"), ZoneId.of("Asia/Seoul"));

    private CareCycleRepository careCycleRepository;
    private SkinAnalysisRepository analysisRepository;
    private SelfCheckRepository selfCheckRepository;
    private UserRepository userRepository;
    private CareCycleService service;
    private User user;
    private SkinAnalysis analysis;

    @BeforeEach
    void setUp() {
        careCycleRepository = mock(CareCycleRepository.class);
        analysisRepository = mock(SkinAnalysisRepository.class);
        selfCheckRepository = mock(SelfCheckRepository.class);
        userRepository = mock(UserRepository.class);
        service = new CareCycleService(careCycleRepository, analysisRepository,
                selfCheckRepository, userRepository, CLOCK);
        user = new User(USER_CODE, "테스트 사용자", "010-1234-1234");
        SkinCapture capture = new SkinCapture(user, LocalDate.of(2026, 8, 17),
                LocalDateTime.of(2026, 8, 17, 10, 0), "/images/face.jpg",
                "face.jpg", "image/jpeg", 3, SkinCaptureQualityStatus.VALID);
        analysis = new SkinAnalysis(capture, LocalDateTime.of(2026, 8, 17, 10, 1),
                80, "Good", "1.0", "{}");
    }

    @Test
    void createsEveningAndNextMorningCycleFromLatestAnalysis() {
        when(userRepository.findByUserCode(USER_CODE)).thenReturn(Optional.of(user));
        when(analysisRepository.findFirstBySkinCapture_User_UserCodeOrderByAnalyzedAtDesc(USER_CODE))
                .thenReturn(Optional.of(analysis));
        when(careCycleRepository.save(any(CareCycle.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        CareCycleResult result = service.create(USER_CODE);

        assertThat(result.careCycle().getOriginCaptureDate()).isEqualTo("2026-08-17");
        assertThat(result.careCycle().getEveningCareDate()).isEqualTo("2026-08-17");
        assertThat(result.careCycle().getMorningCareDate()).isEqualTo("2026-08-18");
        assertThat(result.careCycle().getCreatedAt()).hasToString("2026-08-17T12:30");
        assertThat(result.inherited()).isFalse();
    }

    @Test
    void returnsLatestPreviousCycleAsInheritedOnDayWithoutCapture() {
        CareCycle cycle = new CareCycle(user, analysis, null,
                LocalDate.of(2026, 8, 17), LocalDateTime.of(2026, 8, 17, 12, 30));
        when(userRepository.existsByUserCode(USER_CODE)).thenReturn(true);
        when(careCycleRepository
                .findFirstByUser_UserCodeAndOriginCaptureDateLessThanEqualOrderByOriginCaptureDateDescCreatedAtDesc(
                        USER_CODE, LocalDate.of(2026, 8, 19)))
                .thenReturn(Optional.of(cycle));

        CareCycleResult result = service.getDaily(USER_CODE, LocalDate.of(2026, 8, 19));

        assertThat(result.servedDate()).isEqualTo("2026-08-19");
        assertThat(result.inherited()).isTrue();
        assertThat(result.careCycle()).isSameAs(cycle);
    }

    @Test
    void rejectsDuplicateCycleForSameAnalysis() {
        when(userRepository.findByUserCode(USER_CODE)).thenReturn(Optional.of(user));
        when(analysisRepository.findFirstBySkinCapture_User_UserCodeOrderByAnalyzedAtDesc(USER_CODE))
                .thenReturn(Optional.of(analysis));
        when(careCycleRepository.existsBySkinAnalysis_Id(analysis.getId())).thenReturn(true);

        assertThatThrownBy(() -> service.create(USER_CODE))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.errorCode()).isEqualTo(ErrorCode.CARE_CYCLE_ALREADY_EXISTS));
        verify(careCycleRepository, never()).save(any());
    }
}
