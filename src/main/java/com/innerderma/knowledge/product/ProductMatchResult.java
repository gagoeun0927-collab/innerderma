package com.innerderma.knowledge.product;

import java.util.List;

/**
 * Product Matcher의 결과. Night/Morning 스킨케어 제품과 Inner Care 제품을 분리해 반환한다.
 * Rule Engine이 결정한 step 상한(night_max_steps, morning_max_steps, inner_care_max_items)에 맞게 제한된다.
 */
public record ProductMatchResult(
        List<PieceSeoulProduct> nightProducts,
        List<PieceSeoulProduct> morningProducts,
        List<WimStoreProduct> innerCareProducts,
        String primaryConcern,
        String treatmentCode
) {}
