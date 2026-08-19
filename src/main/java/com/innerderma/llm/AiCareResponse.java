package com.innerderma.llm;

import java.util.List;
import java.util.Map;

/**
 * AI 파이프라인 end-to-end 응답.
 * LLM이 생성한 자연어 응답 + 적용된 규칙 + 검증 결과를 함께 반환한다.
 */
public record AiCareResponse(
        LlmResponse care,
        List<String> appliedRules,
        String primaryConcern,
        String locale,
        boolean validated,
        List<String> validationViolations,
        Map<String, String> productSources
) {}
