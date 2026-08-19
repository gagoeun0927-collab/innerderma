package com.innerderma.knowledge.product;

import java.util.List;

/**
 * WIM Store 이너케어(섭취형) 제품 KB 항목.
 */
public record WimStoreProduct(
        String productId,
        String brand,
        String name,
        String category,
        List<String> stateTags,
        List<String> dietaryTags,
        List<String> allergens,
        List<String> restrictions,
        String usage,
        List<String> verifiedClaims,
        List<String> ingredientsHighlight,
        List<String> warnings,
        boolean isActive,
        Integer price,
        String officialUrl,
        String imageUrl,
        Integer recommendFrequencyDays
) {
    /** 추천 빈도 일수. null이면 1(매일). */
    public int frequencyDays() {
        return recommendFrequencyDays != null ? recommendFrequencyDays : 1;
    }

    public boolean matchesConcern(String taxonomyConcern) {
        if (stateTags == null || stateTags.isEmpty()) return false;
        return stateTags.stream().anyMatch(tag -> tag.equalsIgnoreCase(taxonomyConcern));
    }

    public boolean hasAllergen(List<String> userAllergens) {
        if (allergens == null || allergens.isEmpty() || userAllergens == null) return false;
        return allergens.stream().anyMatch(a ->
                userAllergens.stream().anyMatch(ua -> ua.equalsIgnoreCase(a)));
    }

    public boolean hasRestriction(List<String> userRestrictions) {
        if (restrictions == null || restrictions.isEmpty() || userRestrictions == null) return false;
        return restrictions.stream().anyMatch(r ->
                userRestrictions.stream().anyMatch(ur -> ur.equalsIgnoreCase(r)));
    }
}
