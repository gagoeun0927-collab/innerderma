package com.innerderma.product.api;

import com.innerderma.common.error.BusinessException;
import com.innerderma.common.error.ErrorCode;
import com.innerderma.common.error.GlobalExceptionHandler;
import com.innerderma.product.application.ProductService;
import com.innerderma.product.domain.ProductCategory;
import com.innerderma.product.domain.ProductConcern;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

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
}
