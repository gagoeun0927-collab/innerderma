package com.innerderma.skinanalysis.application;

import com.innerderma.common.error.BusinessException;
import com.innerderma.common.error.ErrorCode;
import com.innerderma.skinanalysis.domain.SkinAnalysis;
import com.innerderma.skinanalysis.domain.SkinAnalysisRepository;
import com.innerderma.skincapture.application.SkinCaptureStorage;
import com.innerderma.skincapture.domain.SkinCapture;
import com.innerderma.skincapture.domain.SkinCaptureQualityStatus;
import com.innerderma.skincapture.domain.SkinCaptureRepository;
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
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SkinAnalysisServiceTest {

    private static final String USER_CODE = "WHS-DEMO-001";
    private static final Clock CLOCK = Clock.fixed(
            Instant.parse("2026-08-17T03:30:00Z"), ZoneId.of("Asia/Seoul")
    );

    private SkinAnalysisRepository analysisRepository;
    private SkinCaptureRepository captureRepository;
    private UserRepository userRepository;
    private SkinCaptureStorage storage;
    private SkinAgeClient client;
    private SkinAnalysisService service;

    @BeforeEach
    void setUp() {
        analysisRepository = mock(SkinAnalysisRepository.class);
        captureRepository = mock(SkinCaptureRepository.class);
        userRepository = mock(UserRepository.class);
        storage = mock(SkinCaptureStorage.class);
        client = mock(SkinAgeClient.class);
        service = new SkinAnalysisService(
                analysisRepository, captureRepository, userRepository, storage, client,
                new ObjectMapper(), CLOCK
        );
    }

    @Test
    void analyzesLatestCaptureAndStoresFullResult() {
        SkinCapture capture = capture();
        SkinAgeAnalysisResult externalResult = validResult();
        when(userRepository.existsByUserCode(USER_CODE)).thenReturn(true);
        when(captureRepository.findFirstByUser_UserCodeOrderByCapturedAtDesc(USER_CODE))
                .thenReturn(Optional.of(capture));
        when(storage.load("/images/face.jpg")).thenReturn(new byte[]{1, 2, 3});
        when(client.analyze(any(), any(), any(), any())).thenReturn(externalResult);
        when(analysisRepository.save(any(SkinAnalysis.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        SkinAnalysisResult result = service.analyze(USER_CODE, null, 24);

        assertThat(result.analysis().getAnalyzedAt()).hasToString("2026-08-17T12:30");
        assertThat(result.analysis().getOverallScore()).isEqualTo(78.5);
        assertThat(result.analysis().getRawResult()).contains("predicted_skin_age", "zone_scores");
        assertThat(result.result().aggregateMetrics().priorityConcerns()).hasSize(1);
        verify(client).analyze(any(), any(), any(), any());
    }

    @Test
    void rejectsInvalidExternalResponseWithoutSaving() {
        SkinCapture capture = capture();
        SkinAgeAnalysisResult invalid = new SkinAgeAnalysisResult(
                validResult().summary(), List.of(), validResult().aggregateMetrics(), null, validResult().metadata()
        );
        when(userRepository.existsByUserCode(USER_CODE)).thenReturn(true);
        when(captureRepository.findFirstByUser_UserCodeOrderByCapturedAtDesc(USER_CODE))
                .thenReturn(Optional.of(capture));
        when(storage.load(any())).thenReturn(new byte[]{1});
        when(client.analyze(any(), any(), any(), any())).thenReturn(invalid);

        assertThatThrownBy(() -> service.analyze(USER_CODE, null, null))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.errorCode()).isEqualTo(ErrorCode.SKINAGE_INVALID_RESPONSE)
                );
        verify(analysisRepository, never()).save(any());
    }

    @Test
    void returnsStoredLatestResultWithoutCallingExternalApi() {
        SkinAnalysis analysis = new SkinAnalysis(
                capture(), LocalDateTime.of(2026, 8, 17, 12, 30),
                78.5, "Good", "1.0.0", write(validResult())
        );
        when(userRepository.existsByUserCode(USER_CODE)).thenReturn(true);
        when(analysisRepository.findFirstBySkinCapture_User_UserCodeOrderByAnalyzedAtDesc(USER_CODE))
                .thenReturn(Optional.of(analysis));

        SkinAnalysisResult result = service.getLatest(USER_CODE);

        assertThat(result.result().summary().overallScore()).isEqualTo(78.5);
        verify(client, never()).analyze(any(), any(), any(), any());
    }

    private SkinCapture capture() {
        return new SkinCapture(
                new User(USER_CODE, "테스트 사용자", "010-1234-1234"),
                LocalDate.of(2026, 8, 17),
                LocalDateTime.of(2026, 8, 17, 12, 0),
                "/images/face.jpg", "face.jpg", "image/jpeg", 3,
                SkinCaptureQualityStatus.VALID
        );
    }

    private SkinAgeAnalysisResult validResult() {
        var concerns = List.of(
                new SkinAgeAnalysisResult.ConcernScore("wrinkle", 80, "minimal"),
                new SkinAgeAnalysisResult.ConcernScore("pore_texture", 60, "moderate"),
                new SkinAgeAnalysisResult.ConcernScore("pigmentation", 70, "mild"),
                new SkinAgeAnalysisResult.ConcernScore("redness", 75, "mild")
        );
        var zone = new SkinAgeAnalysisResult.ZoneScore("forehead", 71.25, "Good", 1.0, concerns);
        return new SkinAgeAnalysisResult(
                new SkinAgeAnalysisResult.Summary(23.2, 24, -0.8, 78.5, "Good"),
                List.of(zone, zone, zone, zone, zone, zone, zone),
                new SkinAgeAnalysisResult.AggregateMetrics(
                        67.4, 70.5,
                        Map.of("wrinkle", 80.0, "pore_texture", 60.0,
                                "pigmentation", 70.0, "redness", 75.0),
                        List.of(new SkinAgeAnalysisResult.PriorityConcernItem(
                                1, "nose", "pore_texture", 42, "significant"))
                ),
                null,
                new SkinAgeAnalysisResult.Metadata(59.9, "1.0.0", "cuda", 512)
        );
    }

    private String write(SkinAgeAnalysisResult result) {
        return new ObjectMapper().writeValueAsString(result);
    }
}
