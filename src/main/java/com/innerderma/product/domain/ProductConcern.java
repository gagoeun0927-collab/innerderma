package com.innerderma.product.domain;

public enum ProductConcern {
    GENERAL, WRINKLE, PORE_TEXTURE, PIGMENTATION, REDNESS, ACNE, BLACKHEAD, DARK_CIRCLE, EYE_SAGGING, SKIN_UNIFORMITY;

    public static ProductConcern fromAnalysisConcern(String concern) {
        if (concern == null) return GENERAL;
        return switch (concern) {
            case "wrinkle" -> WRINKLE;
            case "pore_texture" -> PORE_TEXTURE;
            case "pigmentation" -> PIGMENTATION;
            case "redness" -> REDNESS;
            case "acne" -> ACNE;
            case "blackhead" -> BLACKHEAD;
            case "dark_circle" -> DARK_CIRCLE;
            case "eye_sagging" -> EYE_SAGGING;
            case "skin_uniformity" -> SKIN_UNIFORMITY;
            default -> GENERAL;
        };
    }
}
