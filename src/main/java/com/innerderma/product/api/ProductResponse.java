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
        @JsonInclude(JsonInclude.Include.NON_NULL) String caution,
        @JsonInclude(JsonInclude.Include.NON_NULL) String locale
) {
    /** 번역 없이 기본(한국어) 응답 */
    public static ProductResponse from(Product product) {
        return from(product, null);
    }

    /**
     * 번역이 있으면 name, usage, verifiedClaims를 번역값으로 덮어쓰고
     * caution 필드를 추가한다. locale 필드로 어떤 언어가 적용됐는지 표시한다.
     * 번역이 없으면 기존 한국어 필드를 그대로 내려주고 caution/locale은 null(미포함).
     */
    public static ProductResponse from(Product product, ProductTranslation translation) {
        String resolvedName = product.getName();
        String resolvedUsage = product.getUsage() != null ? product.getUsage() : product.getApplicationMethod();
        List<String> resolvedClaims = parseJson(product.getVerifiedClaimsJson());
        String resolvedCaution = null;
        String resolvedLocale = null;

        if (translation != null) {
            resolvedLocale = translation.getLocale();
            if (translation.getName() != null && !translation.getName().isBlank()) {
                resolvedName = translation.getName();
            }
            if (translation.getUsage() != null && !translation.getUsage().isBlank()) {
                resolvedUsage = translation.getUsage();
            }
            List<String> translatedFeatures = parseJson(translation.getFeaturesJson());
            if (!translatedFeatures.isEmpty()) {
                resolvedClaims = translatedFeatures;
            }
            if (translation.getCaution() != null && !translation.getCaution().isBlank()) {
                resolvedCaution = translation.getCaution();
            }
        }

        return new ProductResponse(
                product.getId(), product.getProductCode(), product.getBrand(),
                resolvedName, product.getCategory(), product.getTargetConcern(),
                product.isSafetyAttentionCompatible(), product.isDemoProduct(),
                product.getOfficialUrl(), product.getSource(), product.getPrice(),
                product.getImageUrl(), resolvedUsage, product.getApplicationMethod(),
                resolvedClaims,
                parseJson(product.getIngredientsHighlightJson()),
                parseJson(product.getSkinStateTagsJson()),
                resolvedCaution,
                resolvedLocale
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
}
