package com.innerderma.product.api;

import com.innerderma.product.domain.*;

import java.util.List;

public record ProductResponse(
        Long id, String productCode, String brand, String name,
        ProductCategory category, ProductConcern targetConcern,
        boolean safetyAttentionCompatible, boolean demoProduct,
        String officialUrl, String source, Integer price, String imageUrl,
        String usage, String applicationMethod,
        List<String> verifiedClaims, List<String> ingredientsHighlight,
        List<String> skinStateTags
) {
    public static ProductResponse from(Product product) {
        return new ProductResponse(
                product.getId(), product.getProductCode(), product.getBrand(),
                product.getName(), product.getCategory(), product.getTargetConcern(),
                product.isSafetyAttentionCompatible(), product.isDemoProduct(),
                product.getOfficialUrl(), product.getSource(), product.getPrice(),
                product.getImageUrl(), product.getUsage(), product.getApplicationMethod(),
                parseJson(product.getVerifiedClaimsJson()),
                parseJson(product.getIngredientsHighlightJson()),
                parseJson(product.getSkinStateTagsJson())
        );
    }

    private static List<String> parseJson(String json) {
        if (json == null || json.isBlank()) return List.of();
        // 간단한 JSON array 파싱: ["a","b"] → List.of("a","b")
        try {
            return new tools.jackson.databind.ObjectMapper()
                    .readValue(json, new tools.jackson.core.type.TypeReference<List<String>>() {});
        } catch (Exception e) {
            return List.of();
        }
    }
}
