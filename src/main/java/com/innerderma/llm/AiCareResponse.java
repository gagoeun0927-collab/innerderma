package com.innerderma.llm;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;
import java.util.Map;

/**
 * AI 파이프라인 end-to-end 응답.
 * LLM이 생성한 자연어 응답 + 적용된 규칙 + 검증 결과를 함께 반환한다.
 *
 * <p>{@code routineWithheld}가 true면 제품 추천이 <b>의도적으로</b> 비어 있다는 뜻이다.
 * Rule Engine이 {@code limit_new_product_addition}을 적용한 경우(시술 회복기, 안전 주의,
 * 상태 악화, 비교 데이터 부족 등)로, 오류가 아니라 "기존 루틴을 유지하라"는 결정이다.
 * 프론트는 이 경우 빈 목록을 오류로 표시하지 말고 {@code routineWithheldReason}을 안내해야 한다.
 */
public record AiCareResponse(
        LlmResponse care,
        List<String> appliedRules,
        String primaryConcern,
        String locale,
        boolean validated,
        List<String> validationViolations,
        Map<String, String> productSources,
        boolean routineWithheld,
        @JsonInclude(JsonInclude.Include.NON_NULL) String routineWithheldReason
) {
    /** 제품 추천이 정상 제공된 응답 */
    public static AiCareResponse of(LlmResponse care, List<String> appliedRules, String primaryConcern,
                                    String locale, boolean validated, List<String> validationViolations,
                                    Map<String, String> productSources) {
        return new AiCareResponse(care, appliedRules, primaryConcern, locale, validated,
                validationViolations, productSources, false, null);
    }

    /** 제품 추천이 규칙에 의해 보류된 응답 */
    public static AiCareResponse withheld(LlmResponse care, List<String> appliedRules, String primaryConcern,
                                          String locale, boolean validated, List<String> validationViolations,
                                          Map<String, String> productSources, String reason) {
        return new AiCareResponse(care, appliedRules, primaryConcern, locale, validated,
                validationViolations, productSources, true, reason);
    }
}
