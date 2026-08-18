package com.innerderma.knowledge.product;

import java.util.List;

/**
 * Piece Seoul 스킨케어 제품 KB 항목.
 */
public record PieceSeoulProduct(
        String productId,
        String brand,
        String name,
        String category,
        List<String> tags,
        List<String> skinStateTags,
        List<String> treatmentCompatibility,
        List<RestrictedTreatment> restrictedAfterTreatments,
        List<String> usageTime,
        String frequency,
        String amount,
        String applicationMethod,
        List<String> warnings,
        List<String> verifiedClaims,
        List<String> ingredientsHighlight,
        List<String> allergens,
        boolean isActive,
        Integer price,
        String officialUrl,
        String imageUrl
) {
    public record RestrictedTreatment(String treatment, int restrictDays) {}

    public boolean isCompatibleWith(String treatmentCode) {
        if (treatmentCompatibility == null || treatmentCompatibility.isEmpty()) return true;
        return treatmentCompatibility.stream()
                .anyMatch(t -> t.equalsIgnoreCase(treatmentCode));
    }

    public boolean matchesConcern(String taxonomyConcern) {
        if (skinStateTags == null || skinStateTags.isEmpty()) return false;
        return skinStateTags.stream()
                .anyMatch(tag -> tag.equalsIgnoreCase(taxonomyConcern));
    }

    public boolean hasRestrictedTag(List<String> restrictedTags) {
        if (tags == null || restrictedTags == null) return false;
        return tags.stream().anyMatch(tag ->
                restrictedTags.stream().anyMatch(r -> r.equalsIgnoreCase(tag)));
    }

    public boolean isForTime(String time) {
        if (usageTime == null || usageTime.isEmpty()) return true;
        return usageTime.stream().anyMatch(t -> t.equalsIgnoreCase(time));
    }
}
