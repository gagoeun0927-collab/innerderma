package com.innerderma.product.api;

import com.innerderma.product.domain.*;

public record ProductResponse(Long id, String productCode, String brand, String name,
                              ProductCategory category, ProductConcern targetConcern,
                              boolean safetyAttentionCompatible, boolean demoProduct,
                              String officialUrl) {
    public static ProductResponse from(Product product) {
        return new ProductResponse(product.getId(), product.getProductCode(), product.getBrand(),
                product.getName(), product.getCategory(), product.getTargetConcern(),
                product.isSafetyAttentionCompatible(), product.isDemoProduct(), product.getOfficialUrl());
    }
}
