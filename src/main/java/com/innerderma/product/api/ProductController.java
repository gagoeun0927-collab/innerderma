package com.innerderma.product.api;

import com.innerderma.common.response.ApiResponse;
import com.innerderma.product.application.ProductService;
import com.innerderma.product.domain.ProductCategory;
import com.innerderma.product.domain.ProductConcern;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Product", description = "제품 카탈로그 조회 API (데모 제품 + Piece Seoul / WIM Store 지식베이스 제품)")
@RestController
@RequestMapping("/api/products")
public class ProductController {
    private final ProductService service;
    public ProductController(ProductService service) { this.service = service; }

    @Operation(
            summary = "제품 목록 조회",
            description = "활성 제품 목록을 조회한다. category, concern, source 파라미터로 필터링할 수 있으며, "
                    + "모두 생략하면 전체 활성 제품을 반환한다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "category/concern 값이 유효하지 않음")
    })
    @GetMapping
    public ApiResponse<List<ProductResponse>> getProducts(
            @Parameter(description = "제품 카테고리 (예: CLEANSER, TONER, SERUM, MOISTURIZER, SUNSCREEN, OIL, MASK, TARGETED_CARE, POWDER, JELLY, SUPPLEMENT, DRINK, FOOD)")
            @RequestParam(value = "category", required = false) ProductCategory category,
            @Parameter(description = "타깃 고민 (예: GENERAL, WRINKLE, PORE_TEXTURE, PIGMENTATION, REDNESS)")
            @RequestParam(value = "concern", required = false) ProductConcern concern,
            @Parameter(description = "판매처 구분. PIECE_SEOUL(스킨케어) 또는 WIM_STORE(섭취류). 생략 시 데모 제품까지 모두 포함")
            @RequestParam(value = "source", required = false) String source) {
        return ApiResponse.success(service.getActiveProducts(category, concern, source));
    }

    @Operation(summary = "제품 상세 조회", description = "productCode로 단일 제품의 상세 정보를 조회한다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "제품을 찾을 수 없음")
    })
    @GetMapping("/{productCode}")
    public ApiResponse<ProductResponse> getProduct(
            @Parameter(description = "제품 코드 (예: PSS_001, WIM_001, DEMO-CLEANSER-001)", required = true)
            @PathVariable String productCode) {
        return ApiResponse.success(service.getProduct(productCode));
    }
}
