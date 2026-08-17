package com.innerderma.productrecommendation.application;

import com.innerderma.product.domain.Product;

public record ProductRecommendationItem(Product product, String usagePhase, String reason) {}
