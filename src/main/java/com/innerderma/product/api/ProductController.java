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
            description = "활성 제품 목록을 조회한다. category, concern, source로 필터링하며, "
                    + "locale 파라미터 또는 Accept-Language 헤더로 다국어 번역을 받을 수 있다. "
                    + "locale이 ko이거나 생략하면 한국어 기본값만 반환하고, en/ja/zh 등을 지정하면 "
                    + "translation 필드에 해당 언어 번역이 포함된다."
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
            @RequestParam(value = "source", required = false) String source,
            @Parameter(description = "응답 언어. en/ja/zh 중 하나를 지정하면 translation 필드에 번역 포함. 생략 또는 ko이면 한국어 기본값만 반환")
            @RequestParam(value = "locale", required = false) String localeParam,
            @Parameter(description = "Accept-Language 헤더로도 언어를 지정할 수 있음 (locale 파라미터가 우선)", hidden = true)
            @RequestHeader(value = "Accept-Language", required = false) String acceptLanguage) {
        String locale = resolveLocale(localeParam, acceptLanguage);
        return ApiResponse.success(service.getActiveProducts(category, concern, source, locale));
    }

    @Operation(
            summary = "제품 상세 조회",
            description = "productCode로 단일 제품의 상세 정보를 조회한다. "
                    + "locale 파라미터 또는 Accept-Language 헤더로 다국어 번역을 받을 수 있다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "제품을 찾을 수 없음")
    })
    @GetMapping("/{productCode}")
    public ApiResponse<ProductResponse> getProduct(
            @Parameter(description = "제품 코드 (예: PSS_001, WIM_001, DEMO-CLEANSER-001)", required = true)
            @PathVariable String productCode,
            @Parameter(description = "응답 언어. en/ja/zh 중 하나를 지정하면 translation 필드에 번역 포함")
            @RequestParam(value = "locale", required = false) String localeParam,
            @Parameter(description = "Accept-Language 헤더로도 언어를 지정할 수 있음 (locale 파라미터가 우선)", hidden = true)
            @RequestHeader(value = "Accept-Language", required = false) String acceptLanguage) {
        String locale = resolveLocale(localeParam, acceptLanguage);
        return ApiResponse.success(service.getProduct(productCode, locale));
    }

    /**
     * locale 쿼리 파라미터가 있으면 그걸 사용하고, 없으면 Accept-Language 헤더에서 1차 언어 추출.
     */
    private String resolveLocale(String localeParam, String acceptLanguage) {
        if (localeParam != null && !localeParam.isBlank()) {
            return localeParam.trim();
        }
        if (acceptLanguage != null && !acceptLanguage.isBlank()) {
            // "en-US,en;q=0.9,ko;q=0.8" → "en-US" → "en"
            String primary = acceptLanguage.split(",")[0].trim();
            int semicolon = primary.indexOf(';');
            if (semicolon > 0) primary = primary.substring(0, semicolon);
            return primary;
        }
        return null;
    }
}
