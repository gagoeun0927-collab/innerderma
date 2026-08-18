package com.innerderma.airule.signal;

import com.innerderma.airule.engine.RuleEvaluationContext;
import com.innerderma.selfcheck.domain.SelfCheckRepository;
import com.innerderma.skinanalysis.domain.SkinAnalysis;
import com.innerderma.skinanalysis.domain.SkinAnalysisRepository;
import com.innerderma.skincapture.domain.SkinCaptureQualityStatus;
import com.innerderma.skincapture.domain.SkinCaptureRepository;
import com.innerderma.skinstate.application.SkinStateSnapshotService;
import com.innerderma.skinstate.domain.SkinStateSnapshotRepository;
import com.innerderma.skinstate.trend.SkinStateTrendService;
import com.innerderma.skinstate.trend.TrendResult;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 최신 Snapshot 과 Trend 결과에서 이미 계산된 결정적 값만 Rule Engine 신호로 변환한다.
 *
 * <p>신호 생성 책임만 가지며 규칙의 의미 해석이나 의료적 판단은 하지 않는다.
 * <ul>
 *   <li>Trend: trend_improving/trend_stable/trend_worsening/trend_unknown ({@link TrendResult#toRuleSignals()})</li>
 *   <li>Snapshot: has_severe_symptom(어느 축이든 ordinal 점수 3이 존재), dominant_&lt;axis&gt;(dominantSymptom 일치 축)</li>
 * </ul>
 * has_severe_symptom 은 "SEVERE 가 입력됐다"는 사실일 뿐 의학적 위험/전문가 진료 필요를 뜻하지 않는다.
 * R000/R002 처럼 결정적 산출 소스가 없는 신호(unusual/rapidly/professional_review/이미지 품질)는 만들지 않는다.
 */
@Component
@Transactional(readOnly = true)
public class SignalAssembler {

    static final String HAS_SEVERE_SYMPTOM = "has_severe_symptom";
    static final String REQUIRES_SAFETY_ATTENTION = "requires_safety_attention";
    static final double CONFIDENCE_THRESHOLD = 0.80;
    private static final int SEVERE_SCORE = 3;

    private final SkinStateSnapshotRepository snapshotRepository;
    private final SelfCheckRepository selfCheckRepository;
    private final SkinCaptureRepository skinCaptureRepository;
    private final SkinAnalysisRepository skinAnalysisRepository;
    private final SkinStateTrendService trendService;
    private final ObjectMapper objectMapper;

    public SignalAssembler(SkinStateSnapshotRepository snapshotRepository,
                           SelfCheckRepository selfCheckRepository,
                           SkinCaptureRepository skinCaptureRepository,
                           SkinAnalysisRepository skinAnalysisRepository,
                           SkinStateTrendService trendService, ObjectMapper objectMapper) {
        this.snapshotRepository = snapshotRepository;
        this.selfCheckRepository = selfCheckRepository;
        this.skinCaptureRepository = skinCaptureRepository;
        this.skinAnalysisRepository = skinAnalysisRepository;
        this.trendService = trendService;
        this.objectMapper = objectMapper;
    }

    public RuleEvaluationContext assemble(String userCode) {
        Map<String, Boolean> signals = new LinkedHashMap<>();

        TrendResult trend = trendService.evaluateLatest(userCode);
        signals.putAll(trend.toRuleSignals());

        snapshotRepository.findFirstByUser_UserCodeOrderBySnapshotDateDesc(userCode).ifPresent(snapshot -> {
            Map<String, Integer> scores = readScores(snapshot.getSymptomScoresJson());
            boolean hasSevere = scores.values().stream().anyMatch(score -> score == SEVERE_SCORE);
            signals.put(HAS_SEVERE_SYMPTOM, hasSevere);
            String dominant = snapshot.getDominantSymptom();
            if (dominant != null && !dominant.isBlank()) {
                signals.put("dominant_" + dominant, true);
            }
        });
        signals.putIfAbsent(HAS_SEVERE_SYMPTOM, false);

        // Safety attention: 기존 SelfCheck.requiresSafetyAttention()을 canonical safety signal로 사용
        boolean safetyAttention = selfCheckRepository
                .findFirstByUser_UserCodeOrderByCheckedAtDesc(userCode)
                .map(selfCheck -> selfCheck.requiresSafetyAttention())
                .orElse(false);
        signals.put(REQUIRES_SAFETY_ATTENTION, safetyAttention);

        // Image quality: 최신 SkinCapture가 QUALITY_CHECK_FAILED이면 R002 신호 생성
        boolean imageQualityFailed = skinCaptureRepository
                .findFirstByUser_UserCodeOrderByCapturedAtDesc(userCode)
                .map(capture -> capture.getQualityStatus() == SkinCaptureQualityStatus.QUALITY_CHECK_FAILED)
                .orElse(false);
        signals.put("image_quality_failed", imageQualityFailed);

        // Confidence: 최신 SkinAnalysis의 zone occlusion_confidence 최솟값 기반
        boolean lowConfidence = computeLowConfidence(userCode);
        signals.put("low_confidence", lowConfidence);

        return RuleEvaluationContext.of(signals);
    }

    /**
     * 최신 SkinAnalysis의 rawResult에서 7개 zone의 occlusion_confidence 최솟값을 추출한다.
     * 최솟값이 0.80 미만이면 low_confidence = true.
     * 분석이 없으면 false (신호 미발생, 보수적이지 않음 — 데이터 없음은 trend_unknown이 처리).
     */
    @SuppressWarnings("unchecked")
    private boolean computeLowConfidence(String userCode) {
        var analysisOpt = skinAnalysisRepository
                .findFirstBySkinCapture_User_UserCodeOrderByAnalyzedAtDesc(userCode);
        if (analysisOpt.isEmpty()) return false;
        try {
            Map<String, Object> root = objectMapper.readValue(analysisOpt.get().getRawResult(), Map.class);
            Object zoneScoresObj = root.get("zone_scores");
            if (!(zoneScoresObj instanceof List<?> zones)) return false;
            double minConfidence = 1.0;
            for (Object zone : zones) {
                if (zone instanceof Map<?, ?> zoneMap) {
                    Object conf = zoneMap.get("occlusion_confidence");
                    if (conf instanceof Number number) {
                        minConfidence = Math.min(minConfidence, number.doubleValue());
                    }
                }
            }
            return minConfidence < CONFIDENCE_THRESHOLD;
        } catch (JacksonException exception) {
            return false;
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Integer> readScores(String json) {
        Map<String, Integer> scores = new LinkedHashMap<>();
        try {
            Map<String, Object> raw = objectMapper.readValue(json, Map.class);
            for (String axis : SkinStateSnapshotService.AXIS_ORDER) {
                Object value = raw.get(axis);
                scores.put(axis, value instanceof Number number ? number.intValue() : 0);
            }
        } catch (JacksonException exception) {
            for (String axis : SkinStateSnapshotService.AXIS_ORDER) {
                scores.put(axis, 0);
            }
        }
        return scores;
    }
}
