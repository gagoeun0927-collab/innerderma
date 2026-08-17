package com.innerderma.productrecommendation.api;

import com.innerderma.caresolution.domain.SafetyLevel;
import com.innerderma.product.api.ProductResponse;
import com.innerderma.productrecommendation.application.*;

import java.time.LocalDate;
import java.util.List;

public record ProductRecommendationResponse(LocalDate originCaptureDate, LocalDate servedDate,
                                            boolean inherited, SafetyLevel safetyLevel,
                                            List<Item> products, String notice) {
    public static ProductRecommendationResponse from(ProductRecommendationResult result) {
        return new ProductRecommendationResponse(result.originCaptureDate(), result.servedDate(),
                result.inherited(), result.safetyLevel(), result.items().stream().map(Item::from).toList(),
                result.notice());
    }

    public record Item(ProductResponse product, String usagePhase, String reason) {
        static Item from(ProductRecommendationItem item) {
            return new Item(ProductResponse.from(item.product()), item.usagePhase(), item.reason());
        }
    }
}
