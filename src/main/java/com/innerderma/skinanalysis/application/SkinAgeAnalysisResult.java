package com.innerderma.skinanalysis.application;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

public record SkinAgeAnalysisResult(
        Summary summary,
        @JsonProperty("zone_scores") List<ZoneScore> zoneScores,
        @JsonProperty("aggregate_metrics") AggregateMetrics aggregateMetrics,
        Heatmaps heatmaps,
        Metadata metadata
) {
    public record Summary(
            @JsonProperty("predicted_skin_age") double predictedSkinAge,
            @JsonProperty("actual_age") Integer actualAge,
            @JsonProperty("age_delta") Double ageDelta,
            @JsonProperty("overall_score") double overallScore,
            @JsonProperty("skin_health_grade") String skinHealthGrade
    ) {}

    public record ConcernScore(String concern, double score, String severity) {}

    public record ZoneScore(
            String zone,
            @JsonProperty("composite_score") double compositeScore,
            String label,
            @JsonProperty("occlusion_confidence") double occlusionConfidence,
            List<ConcernScore> concerns
    ) {}

    public record PriorityConcernItem(
            int rank,
            String zone,
            String concern,
            double score,
            String severity
    ) {}

    public record AggregateMetrics(
            @JsonProperty("t_zone_score") double tZoneScore,
            @JsonProperty("u_zone_score") double uZoneScore,
            @JsonProperty("concern_averages") Map<String, Double> concernAverages,
            @JsonProperty("priority_concerns") List<PriorityConcernItem> priorityConcerns
    ) {}

    public record Heatmaps(
            String wrinkle,
            @JsonProperty("pore_texture") String poreTexture,
            String pigmentation,
            String redness
    ) {}

    public record Metadata(
            @JsonProperty("processing_time_ms") double processingTimeMs,
            @JsonProperty("model_version") String modelVersion,
            String device,
            @JsonProperty("input_size") int inputSize
    ) {}
}
