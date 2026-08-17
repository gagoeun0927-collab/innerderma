package com.innerderma.productrecommendation.api;

import com.innerderma.common.response.ApiResponse;
import com.innerderma.productrecommendation.application.ProductRecommendationService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/users/{userCode}/product-recommendations")
public class ProductRecommendationController {
    private final ProductRecommendationService service;
    public ProductRecommendationController(ProductRecommendationService service) { this.service = service; }

    @GetMapping("/daily")
    public ApiResponse<ProductRecommendationResponse> getDaily(
            @PathVariable String userCode,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ApiResponse.success(ProductRecommendationResponse.from(service.getDaily(userCode, date)));
    }
}
