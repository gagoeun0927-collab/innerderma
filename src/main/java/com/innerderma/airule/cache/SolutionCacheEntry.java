package com.innerderma.airule.cache;

import com.innerderma.airule.solution.SolutionObject;
import com.innerderma.knowledge.product.ProductMatchResult;

import java.time.LocalDateTime;

/**
 * 캐시에 저장되는 Solution 항목. Solution Object + 매칭된 제품 + 생성 시각.
 */
public record SolutionCacheEntry(
        SolutionObject solution,
        ProductMatchResult products,
        LocalDateTime createdAt
) {}
