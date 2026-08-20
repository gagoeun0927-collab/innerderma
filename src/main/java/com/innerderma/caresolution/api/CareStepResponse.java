package com.innerderma.caresolution.api;

/**
 * 구조화된 케어 스텝 응답.
 * @param title 카드 제목 (짧게)
 * @param tagKey 태그 색상 코드 — moist/nutrient/barrier/lock/waste/uv
 * @param tag 화면에 보일 태그 라벨
 * @param description 상세 설명 문구
 */
public record CareStepResponse(
        String title,
        String tagKey,
        String tag,
        String description
) {
    /**
     * 기존 단순 문자열 스텝을 구조화된 객체로 변환.
     * description에서 키워드를 분석해 tagKey/tag를 자동 매핑.
     */
    public static CareStepResponse fromLegacyString(String step, int index) {
        String tagKey = inferTagKey(step);
        String tag = tagToLabel(tagKey);
        String title = generateTitle(step, index);
        return new CareStepResponse(title, tagKey, tag, step);
    }

    private static String inferTagKey(String step) {
        String lower = step.toLowerCase();
        if (lower.contains("자외선") || lower.contains("차단") || lower.contains("썬")) return "uv";
        if (lower.contains("세안") || lower.contains("클렌") || lower.contains("노폐물")) return "waste";
        if (lower.contains("보습") || lower.contains("수분") || lower.contains("토너")) return "moist";
        if (lower.contains("앰플") || lower.contains("세럼") || lower.contains("영양") || lower.contains("비타민")) return "nutrient";
        if (lower.contains("크림") || lower.contains("밤") || lower.contains("오일") || lower.contains("잠금")) return "lock";
        if (lower.contains("장벽") || lower.contains("barrier") || lower.contains("보호")) return "barrier";
        return "moist";
    }

    private static String tagToLabel(String tagKey) {
        return switch (tagKey) {
            case "moist" -> "수분 공급";
            case "nutrient" -> "성분 공급";
            case "barrier" -> "보습막 형성";
            case "lock" -> "수분 잠금";
            case "waste" -> "노폐물 제거";
            case "uv" -> "자외선 차단";
            default -> "수분 공급";
        };
    }

    private static String generateTitle(String step, int index) {
        // 첫 20자 또는 첫 문장을 제목으로 사용
        String trimmed = step.length() <= 20 ? step : step.substring(0, 20) + "...";
        if (step.contains(".")) {
            trimmed = step.substring(0, step.indexOf(".") + 1);
            if (trimmed.length() > 25) trimmed = trimmed.substring(0, 25) + "...";
        }
        return trimmed;
    }
}
