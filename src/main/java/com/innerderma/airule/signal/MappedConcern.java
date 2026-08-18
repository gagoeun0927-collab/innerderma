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
        String concern = mapToConcern(dominantSymptom);
        if (concern == null) {
            return null;
        }
        // Self-report 단독 소스의 기본 confidence: 0.72 (향후 Image Analysis 추가 시 상향)
        return new MappedConcern(concern, 0.72, List.of("SELF_REPORT"));
    }

    private static String mapToConcern(String axis) {
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
}
