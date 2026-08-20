package com.innerderma.skinanalysis.api;

import com.innerderma.skinanalysis.application.SkinAgeAnalysisResult;
import com.innerderma.skinanalysis.application.SkinAnalysisResult;

import java.time.LocalDateTime;

public record SkinAnalysisResponse(
        Long id,
        Long captureId,
        String userCode,
        LocalDateTime analyzedAt,
        MetricScores metricScores,
        SkinAgeAnalysisResult result
) {
    public static SkinAnalysisResponse from(SkinAnalysisResult analysisResult) {
        var analysis = analysisResult.analysis();
        var capture = analysis.getSkinCapture();
        var scores = MetricScores.from(analysisResult.result());
        return new SkinAnalysisResponse(
                analysis.getId(),
                capture.getId(),
                capture.getUser().getUserCode(),
                analysis.getAnalyzedAt(),
                scores,
                analysisResult.result()
        );
    }

    /**
     * 프론트 레이더 차트용 5항목 수치 점수 (0~100).
     * 점수가 높을수록 건강한 상태를 의미한다.
     * SkinAge API의 aggregate_metrics.concern_averages에서 추출한 얼굴 전체 평균.
     *
     * <p>SkinAge는 pore와 texture를 합산한 pore_texture 하나로 제공하므로,
     * poreScore와 textureScore는 동일 값이다.
     * 프론트가 5개 축이 필요하면 둘 다 같은 값을 사용하고,
     * 4개로 줄이려면 poreTextureScore만 쓰면 된다.
     */
    public record MetricScores(
            Double pigmentationScore,
            Double poreTextureScore,
            Double wrinkleScore,
            Double rednessScore,
            Double overallScore
    ) {
        public static MetricScores from(SkinAgeAnalysisResult result) {
            if (result == null || result.aggregateMetrics() == null
                    || result.aggregateMetrics().concernAverages() == null) {
                return null;
            }
            var averages = result.aggregateMetrics().concernAverages();
            return new MetricScores(
                    averages.get("pigmentation"),
                    averages.get("pore_texture"),
                    averages.get("wrinkle"),
                    averages.get("redness"),
                    result.summary() != null ? result.summary().overallScore() : null
            );
        }
    }
}
