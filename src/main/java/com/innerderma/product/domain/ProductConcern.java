package com.innerderma.product.domain;

public enum ProductConcern {
    GENERAL, WRINKLE, PORE_TEXTURE, PIGMENTATION, REDNESS;

    public static ProductConcern fromAnalysisConcern(String concern) {
        if (concern == null) return GENERAL;
        return switch (concern) {
            case "wrinkle" -> WRINKLE;
            case "pore_texture" -> PORE_TEXTURE;
            case "pigmentation" -> PIGMENTATION;
            case "redness" -> REDNESS;
            default -> GENERAL;
        };
    }
}
