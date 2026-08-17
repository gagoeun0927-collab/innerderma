package com.innerderma.product.api;

import com.innerderma.common.response.ApiResponse;
import com.innerderma.product.application.ProductService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/products")
public class ProductController {
    private final ProductService service;
    public ProductController(ProductService service) { this.service = service; }

    @GetMapping
    public ApiResponse<List<ProductResponse>> getProducts() {
        return ApiResponse.success(service.getActiveProducts());
    }
}
