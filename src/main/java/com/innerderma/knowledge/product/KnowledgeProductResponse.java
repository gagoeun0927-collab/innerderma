package com.innerderma.knowledge.product;

import java.util.List;

public record KnowledgeProductResponse(
        String productId,
        String source,
        String brand,
        String name,
        String category,
        List<String> concerns,
        String usage,
        String applicationMethod,
        List<String> verifiedClaims,
        List<String> ingredientsHighlight,
        List<String> warnings,
        boolean isActive,
        Integer price,
        String officialUrl,
        String imageUrl
) {
    public static KnowledgeProductResponse fromPieceSeoul(PieceSeoulProduct p) {
        return new KnowledgeProductResponse(
                p.productId(), "PIECE_SEOUL", p.brand(), p.name(), p.category(),
                p.skinStateTags(), p.frequency(), p.applicationMethod(),
                p.verifiedClaims(), p.ingredientsHighlight(), p.warnings(),
                p.isActive(), p.price(), p.officialUrl(), p.imageUrl()
        );
    }

    public static KnowledgeProductResponse fromWimStore(WimStoreProduct p) {
        return new KnowledgeProductResponse(
                p.productId(), "WIM_STORE", p.brand(), p.name(), p.category(),
                p.stateTags(), p.usage(), null,
                p.verifiedClaims(), p.ingredientsHighlight(), p.warnings(),
                p.isActive(), p.price(), p.officialUrl(), p.imageUrl()
        );
    }
}
