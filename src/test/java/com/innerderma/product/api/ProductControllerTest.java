package com.innerderma.product.api;

import com.innerderma.common.error.BusinessException;
import com.innerderma.common.error.ErrorCode;
import com.innerderma.common.error.GlobalExceptionHandler;
import com.innerderma.product.application.ProductService;
import com.innerderma.product.domain.Product;
import com.innerderma.product.domain.ProductCategory;
import com.innerderma.product.domain.ProductConcern;
import com.innerderma.product.domain.ProductTranslation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ProductControllerTest {

    private ProductService service;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        service = mock(ProductService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new ProductController(service))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void returnsProductDetail() throws Exception {
        when(service.getProduct(eq("PRD-001"), isNull())).thenReturn(ProductResponse.from(sampleProduct()));

        mockMvc.perform(get("/api/products/PRD-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.productCode").value("DEMO-REDNESS-001"))
                .andExpect(jsonPath("$.data.locale").doesNotExist())
                .andExpect(jsonPath("$.data.caution").doesNotExist());
    }

    @Test
    void returnsProductWithLocaleTranslation() throws Exception {
        ProductTranslation tr = new ProductTranslation(
                "DEMO-REDNESS-001", "en", "Redness Calming Care",
                "Apply after cleansing", "[\"Soothes redness\",\"Calms skin\"]", "For external use only."
        );
        when(service.getProduct(eq("DEMO-REDNESS-001"), eq("en")))
                .thenReturn(ProductResponse.from(sampleProduct(), tr));

        mockMvc.perform(get("/api/products/DEMO-REDNESS-001").param("locale", "en"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.locale").value("en"))
                .andExpect(jsonPath("$.data.name").value("Redness Calming Care"))
                .andExpect(jsonPath("$.data.usage").value("Apply after cleansing"))
                .andExpect(jsonPath("$.data.verifiedClaims[0]").value("Soothes redness"))
                .andExpect(jsonPath("$.data.caution").value("For external use only."));
    }

    @Test
    void acceptLanguageHeaderIsUsedWhenNoLocaleParam() throws Exception {
        when(service.getProduct(eq("DEMO-REDNESS-001"), eq("ja")))
                .thenReturn(ProductResponse.from(sampleProduct()));

        mockMvc.perform(get("/api/products/DEMO-REDNESS-001")
                        .header("Accept-Language", "ja,en;q=0.9"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void localeParamTakesPrecedenceOverHeader() throws Exception {
        when(service.getProduct(eq("DEMO-REDNESS-001"), eq("zh")))
                .thenReturn(ProductResponse.from(sampleProduct()));

        mockMvc.perform(get("/api/products/DEMO-REDNESS-001")
                        .param("locale", "zh")
                        .header("Accept-Language", "ja"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void returnsNotFoundWhenProductMissing() throws Exception {
        when(service.getProduct(eq("PRD-999"), isNull()))
                .thenThrow(new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));

        mockMvc.perform(get("/api/products/PRD-999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("PRODUCT_001"));
    }

    @Test
    void forwardsCategoryAndConcernFilters() throws Exception {
        when(service.getActiveProducts(eq(ProductCategory.TARGETED_CARE), eq(ProductConcern.REDNESS), isNull(), isNull()))
                .thenReturn(List.of(ProductResponse.from(sampleProduct())));

        mockMvc.perform(get("/api/products")
                        .param("category", "TARGETED_CARE")
                        .param("concern", "REDNESS"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].productCode").value("DEMO-REDNESS-001"));
    }

    @Test
    void forwardsSourceFilter() throws Exception {
        when(service.getActiveProducts(isNull(), isNull(), eq("PIECE_SEOUL"), isNull()))
                .thenReturn(List.of(ProductResponse.from(sampleProduct())));

        mockMvc.perform(get("/api/products").param("source", "PIECE_SEOUL"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].productCode").value("DEMO-REDNESS-001"));
    }

    @Test
    void forwardsLocaleToListEndpoint() throws Exception {
        when(service.getActiveProducts(isNull(), isNull(), isNull(), eq("en")))
                .thenReturn(List.of(ProductResponse.from(sampleProduct())));

        mockMvc.perform(get("/api/products").param("locale", "en"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void rejectsUnknownCategoryAsBadRequest() throws Exception {
        mockMvc.perform(get("/api/products").param("category", "NOT_A_CATEGORY"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON_001"));
    }

    private Product sampleProduct() {
        return new Product("DEMO-REDNESS-001", "[데모]", "홍조 진정 케어",
                ProductCategory.TARGETED_CARE, ProductConcern.REDNESS, false, true, true, null, 43);
    }
}
