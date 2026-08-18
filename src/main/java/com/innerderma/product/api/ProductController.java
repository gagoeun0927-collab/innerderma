package com.innerderma.product.api;

import com.innerderma.common.response.ApiResponse;
import com.innerderma.product.application.ProductService;
import com.innerderma.product.domain.ProductCategory;
import com.innerderma.product.domain.ProductConcern;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/products")
public class ProductController {
    private final ProductService service;
    public ProductController(ProductService service) { this.service = service; }

    @GetMapping
    public ApiResponse<List<ProductResponse>> getProducts(
            @RequestParam(value = "category", required = false) ProductCategory category,
            @RequestParam(value = "concern", required = false) ProductConcern concern) {
        return ApiResponse.success(service.getActiveProducts(category, concern));
    }
}
