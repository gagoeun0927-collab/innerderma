package com.innerderma.product.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.innerderma.product.domain.*;

import java.util.List;

public record ProductResponse(
        Long id, String productCode, String brand, String name,
        ProductCategory category, ProductConcern targetConcern,
        boolean safetyAttentionCompatible, boolean demoProduct,
        String officialUrl, String source, Integer price, String imageUrl,
        String usage, String applicationMethod,
        List<String> verifiedClaims, List<String> ingredientsHighlight,
        List<String> skinStateTags,
        @JsonInclude(JsonInclude.Include.NON_NULL) TranslationResponse translation
) {
    /** 번역 없이 기본(한국어) 응답 */
    public static ProductResponse from(Product product) {
        return from(product, null);
    }

    /** 번역이 있으면 translation 필드에 포함 */
    public static ProductResponse from(Product product, ProductTranslation translation) {
        TranslationResponse tr = translation != null
                ? new TranslationResponse(
                        translation.getLocale(),
                        translation.getName(),
                        translation.getUsage(),
                        parseJson(translation.getFeaturesJson()),
                        translation.getCaution())
                : null;

        return new ProductResponse(
                product.getId(), product.getProductCode(), product.getBrand(),
                product.getName(), product.getCategory(), product.getTargetConcern(),
                product.isSafetyAttentionCompatible(), product.isDemoProduct(),
                product.getOfficialUrl(), product.getSource(), product.getPrice(),
                product.getImageUrl(), product.getUsage(), product.getApplicationMethod(),
                parseJson(product.getVerifiedClaimsJson()),
                parseJson(product.getIngredientsHighlightJson()),
                parseJson(product.getSkinStateTagsJson()),
                tr
        );
    }

    private static List<String> parseJson(String json) {
        if (json == null || json.isBlank()) return List.of();
        try {
            return new tools.jackson.databind.ObjectMapper()
                    .readValue(json, new tools.jackson.core.type.TypeReference<List<String>>() {});
        } catch (Exception e) {
            return List.of();
        }
    }

    public record TranslationResponse(
            String locale,
            String name,
            String usage,
            List<String> features,
            String caution
    ) {}
}
