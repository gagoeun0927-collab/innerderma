package com.innerderma.product.api;

import com.innerderma.common.error.BusinessException;
import com.innerderma.common.error.ErrorCode;
import com.innerderma.common.error.GlobalExceptionHandler;
import com.innerderma.product.application.ProductService;
import com.innerderma.product.domain.Product;
import com.innerderma.product.domain.ProductCategory;
import com.innerderma.product.domain.ProductConcern;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

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
        when(service.getProduct("PRD-001")).thenReturn(new ProductResponse(
                1L, "PRD-001", "이너덤", "수분 크림", ProductCategory.MOISTURIZER,
                ProductConcern.GENERAL, true, false, "https://example.com/prd-001"));

        mockMvc.perform(get("/api/products/PRD-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.productCode").value("PRD-001"))
                .andExpect(jsonPath("$.data.name").value("수분 크림"));
    }

    @Test
    void returnsNotFoundWhenProductMissing() throws Exception {
        when(service.getProduct("PRD-999"))
                .thenThrow(new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));

        mockMvc.perform(get("/api/products/PRD-999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("PRODUCT_001"));
    }

    @Test
    void forwardsCategoryAndConcernFilters() throws Exception {
        when(service.getActiveProducts(ProductCategory.TARGETED_CARE, ProductConcern.REDNESS))
                .thenReturn(List.of(ProductResponse.from(product())));

        mockMvc.perform(get("/api/products")
                        .param("category", "TARGETED_CARE")
                        .param("concern", "REDNESS"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].productCode").value("DEMO-REDNESS-001"));
    }

    @Test
    void rejectsUnknownCategoryAsBadRequest() throws Exception {
        mockMvc.perform(get("/api/products").param("category", "NOT_A_CATEGORY"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON_001"));
    }

    private Product product() {
        return new Product("DEMO-REDNESS-001", "[데모]", "홍조 진정 케어",
                ProductCategory.TARGETED_CARE, ProductConcern.REDNESS, false, true, true, null, 43);
    }
}
