package com.innerderma.airule.signal;

import java.util.List;

/**
 * 자가문진 dominant symptom 을 피부 상태 taxonomy concern 으로 매핑한 결과.
 * source 와 confidence 를 동반하여 향후 Image Analysis 등 복수 소스 결합을 지원한다.
 * 이것은 diagnosis 가 아니라 self-reported concern 이다.
 */
public record MappedConcern(String concern, double confidence, List<String> sources) {

    /** 자가문진 dominant 축 → Taxonomy concern. 매핑이 없으면 null 반환. */
    public static MappedConcern fromSelfReport(String dominantSymptom) {
        if (dominantSymptom == null || dominantSymptom.isBlank()) {
            return null;
        }
        String concern = mapSelfReportToConcern(dominantSymptom);
        if (concern == null) {
            return null;
        }
        return new MappedConcern(concern, 0.72, List.of("SELF_REPORT"));
    }

    /** SkinAge concern_averages → Taxonomy concern. 가장 낮은 점수(=가장 심한 문제)를 primary concern으로. */
    public static MappedConcern fromImageAnalysis(java.util.Map<String, Double> concernAverages) {
        if (concernAverages == null || concernAverages.isEmpty()) {
            return null;
        }
        String worstConcern = null;
        double worstScore = Double.MAX_VALUE;
        for (var entry : concernAverages.entrySet()) {
            String key = entry.getKey();
            if ("wrinkle".equals(key)) continue; // wrinkle은 시술 영역이라 스킵
            if (entry.getValue() < worstScore) {
                worstScore = entry.getValue();
                worstConcern = key;
            }
        }
        if (worstConcern == null) return null;
        String taxonomy = mapAnalysisConcernToTaxonomy(worstConcern);
        if (taxonomy == null) return null;
        // confidence는 SkinAge 전체 점수 기반이 아니라 고정값 0.85 (이미지 분석 단독)
        return new MappedConcern(taxonomy, 0.85, List.of("IMAGE_ANALYSIS"));
    }

    /** 자가문진 + 이미지 분석 결합. 같은 concern이면 confidence 상향, 다르면 이미지 분석 우선. */
    public static MappedConcern combine(MappedConcern selfReport, MappedConcern imageAnalysis) {
        if (selfReport == null) return imageAnalysis;
        if (imageAnalysis == null) return selfReport;
        if (selfReport.concern().equals(imageAnalysis.concern())) {
            return new MappedConcern(selfReport.concern(), 0.92, List.of("SELF_REPORT", "IMAGE_ANALYSIS"));
        }
        // 서로 다르면 이미지 분석 우선(객관적 데이터)
        return new MappedConcern(imageAnalysis.concern(), imageAnalysis.confidence(),
                List.of("SELF_REPORT", "IMAGE_ANALYSIS"));
    }

    private static String mapSelfReportToConcern(String axis) {
        return switch (axis) {
            case "dryness", "tightness" -> "HYDRATION";
            case "heatSensation", "pain" -> "IRRITATION";
            case "breakout" -> "ACNE";
            case "swelling" -> "SWELLING";
            case "peeling" -> "BARRIER_RECOVERY";
            case "itching" -> "REDNESS";
            default -> null;
        };
    }

    private static String mapAnalysisConcernToTaxonomy(String skinAgeConcern) {
        return switch (skinAgeConcern) {
            case "redness" -> "REDNESS";
            case "pigmentation" -> "PIGMENTATION";
            case "pore_texture" -> "BARRIER_RECOVERY";
            default -> null;
        };
    }
}
