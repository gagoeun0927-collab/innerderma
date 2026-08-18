package com.innerderma.product.application;

import com.innerderma.common.error.BusinessException;
import com.innerderma.common.error.ErrorCode;
import com.innerderma.product.api.ProductResponse;
import com.innerderma.product.domain.Product;
import com.innerderma.product.domain.ProductCategory;
import com.innerderma.product.domain.ProductConcern;
import com.innerderma.product.domain.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ProductServiceTest {

    private ProductRepository repository;
    private ProductService service;

    @BeforeEach
    void setUp() {
        repository = mock(ProductRepository.class);
        service = new ProductService(repository);
    }

    @Test
    void returnsProductByCode() {
        Product product = new Product("PRD-001", "이너덤", "수분 크림",
                ProductCategory.MOISTURIZER, ProductConcern.GENERAL,
                true, true, false, "https://example.com/prd-001", 1);
        when(repository.findByProductCode("PRD-001")).thenReturn(Optional.of(product));

        ProductResponse result = service.getProduct("PRD-001");

        assertThat(result.productCode()).isEqualTo("PRD-001");
        assertThat(result.name()).isEqualTo("수분 크림");
        assertThat(result.category()).isEqualTo(ProductCategory.MOISTURIZER);
    }

    @Test
    void throwsProductNotFoundWhenMissing() {
        when(repository.findByProductCode("PRD-999")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getProduct("PRD-999"))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.errorCode()).isEqualTo(ErrorCode.PRODUCT_NOT_FOUND)
                );
    }
}
