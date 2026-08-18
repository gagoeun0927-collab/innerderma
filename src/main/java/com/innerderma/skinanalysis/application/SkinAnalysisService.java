package com.innerderma.skinanalysis.application;

import com.innerderma.airule.cache.SolutionCache;
import com.innerderma.common.error.BusinessException;
import com.innerderma.common.error.ErrorCode;
import com.innerderma.skinanalysis.domain.SkinAnalysis;
import com.innerderma.skinanalysis.domain.SkinAnalysisRepository;
import com.innerderma.skincapture.application.SkinCaptureStorage;
import com.innerderma.skincapture.domain.SkinCapture;
import com.innerderma.skincapture.domain.SkinCaptureRepository;
import com.innerderma.user.domain.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Set;

@Service
@Transactional(readOnly = true)
public class SkinAnalysisService {

    private static final ZoneId MVP_ZONE = ZoneId.of("Asia/Seoul");
    private static final long MAX_RANGE_DAYS = 31;
    private static final Set<String> REQUIRED_CONCERNS = Set.of(
            "wrinkle", "pore_texture", "pigmentation", "redness"
    );

    private final SkinAnalysisRepository analysisRepository;
    private final SkinCaptureRepository captureRepository;
    private final UserRepository userRepository;
    private final SkinCaptureStorage storage;
    private final SkinAgeClient skinAgeClient;
    private final ObjectMapper objectMapper;
    private final SolutionCache solutionCache;
    private final Clock clock;

    @Autowired
    public SkinAnalysisService(
            SkinAnalysisRepository analysisRepository,
            SkinCaptureRepository captureRepository,
            UserRepository userRepository,
            SkinCaptureStorage storage,
            SkinAgeClient skinAgeClient,
            ObjectMapper objectMapper,
            SolutionCache solutionCache
    ) {
        this(analysisRepository, captureRepository, userRepository, storage, skinAgeClient,
                objectMapper, solutionCache, Clock.system(MVP_ZONE));
    }

    SkinAnalysisService(
            SkinAnalysisRepository analysisRepository,
            SkinCaptureRepository captureRepository,
            UserRepository userRepository,
            SkinCaptureStorage storage,
            SkinAgeClient skinAgeClient,
            ObjectMapper objectMapper,
            SolutionCache solutionCache,
            Clock clock
    ) {
        this.analysisRepository = analysisRepository;
        this.captureRepository = captureRepository;
        this.userRepository = userRepository;
        this.storage = storage;
        this.skinAgeClient = skinAgeClient;
        this.objectMapper = objectMapper;
        this.solutionCache = solutionCache;
        this.clock = clock;
    }

    @Transactional
    public SkinAnalysisResult analyze(String userCode, Long captureId, Integer actualAge) {
        if (!userRepository.existsByUserCode(userCode)) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        SkinCapture capture = captureId == null
                ? captureRepository.findFirstByUser_UserCodeOrderByCapturedAtDesc(userCode)
                        .orElseThrow(() -> new BusinessException(ErrorCode.SKIN_CAPTURE_NOT_FOUND))
                : captureRepository.findByIdAndUser_UserCode(captureId, userCode)
                        .orElseThrow(() -> new BusinessException(ErrorCode.SKIN_CAPTURE_NOT_FOUND));

        if (analysisRepository.existsBySkinCapture_Id(capture.getId())) {
            throw new BusinessException(ErrorCode.SKIN_ANALYSIS_ALREADY_EXISTS);
        }

        byte[] imageBytes = storage.load(capture.getImagePath());
        SkinAgeAnalysisResult result = skinAgeClient.analyze(
                imageBytes,
                capture.getOriginalFilename(),
                capture.getContentType(),
                actualAge
        );
        validate(result);

        SkinAnalysis analysis = new SkinAnalysis(
                capture,
                LocalDateTime.now(clock),
                result.summary().overallScore(),
                result.summary().skinHealthGrade(),
                result.metadata().modelVersion(),
                serialize(result)
        );
        SkinAnalysis saved = analysisRepository.save(analysis);
        solutionCache.invalidate(userCode);
        return new SkinAnalysisResult(saved, result);
    }

    public SkinAnalysisResult getLatest(String userCode) {
        if (!userRepository.existsByUserCode(userCode)) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        SkinAnalysis analysis = analysisRepository
                .findFirstBySkinCapture_User_UserCodeOrderByAnalyzedAtDesc(userCode)
                .orElseThrow(() -> new BusinessException(ErrorCode.SKIN_ANALYSIS_NOT_FOUND));
        return new SkinAnalysisResult(analysis, deserialize(analysis.getRawResult()));
    }

    public SkinAnalysisHistoryResult getHistory(String userCode, LocalDate from, LocalDate to) {
        if (!userRepository.existsByUserCode(userCode)) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        LocalDate resolvedTo = to == null ? LocalDate.now(clock) : to;
        LocalDate resolvedFrom = from == null ? resolvedTo.minusDays(29) : from;
        if (resolvedFrom.isAfter(resolvedTo)
                || ChronoUnit.DAYS.between(resolvedFrom, resolvedTo) + 1 > MAX_RANGE_DAYS) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }
        var items = analysisRepository.findBySkinCapture_User_UserCodeAndAnalyzedAtBetweenOrderByAnalyzedAtDesc(
                userCode, resolvedFrom.atStartOfDay(), resolvedTo.atTime(LocalTime.MAX));
        return new SkinAnalysisHistoryResult(resolvedFrom, resolvedTo, items);
    }

    private void validate(SkinAgeAnalysisResult result) {
        if (result.summary() == null || result.aggregateMetrics() == null || result.metadata() == null
                || result.zoneScores() == null || result.zoneScores().size() != 7
                || !validScore(result.summary().overallScore())
                || result.summary().skinHealthGrade() == null
                || result.summary().skinHealthGrade().isBlank()
                || result.metadata().modelVersion() == null
                || result.metadata().modelVersion().isBlank()
                || result.aggregateMetrics().concernAverages() == null
                || !result.aggregateMetrics().concernAverages().keySet().containsAll(REQUIRED_CONCERNS)
                || result.zoneScores().stream().anyMatch(zone ->
                        zone == null || zone.zone() == null || !validScore(zone.compositeScore())
                                || zone.occlusionConfidence() < 0.1 || zone.occlusionConfidence() > 1.0
                                || zone.concerns() == null || zone.concerns().size() != 4
                                || zone.concerns().stream().anyMatch(concern -> concern == null
                                        || !REQUIRED_CONCERNS.contains(concern.concern())
                                        || !validScore(concern.score()) || concern.severity() == null))) {
            throw new BusinessException(ErrorCode.SKINAGE_INVALID_RESPONSE);
        }
    }

    private boolean validScore(double score) {
        return Double.isFinite(score) && score >= 0 && score <= 100;
    }

    private String serialize(SkinAgeAnalysisResult result) {
        try {
            return objectMapper.writeValueAsString(result);
        } catch (JacksonException exception) {
            throw new BusinessException(ErrorCode.SKINAGE_INVALID_RESPONSE);
        }
    }

    private SkinAgeAnalysisResult deserialize(String rawResult) {
        try {
            return objectMapper.readValue(rawResult, SkinAgeAnalysisResult.class);
        } catch (JacksonException exception) {
            throw new BusinessException(ErrorCode.SKINAGE_INVALID_RESPONSE);
        }
    }
}
