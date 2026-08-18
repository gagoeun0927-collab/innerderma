package com.innerderma.product.application;

import com.innerderma.product.api.ProductResponse;
import com.innerderma.product.domain.Product;
import com.innerderma.product.domain.ProductCategory;
import com.innerderma.product.domain.ProductConcern;
import com.innerderma.product.domain.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ProductServiceTest {

    private ProductRepository repository;
    private ProductService service;

    @BeforeEach
    void setUp() {
        repository = mock(ProductRepository.class);
        service = new ProductService(repository);
        when(repository.findAllByActiveTrueOrderByDisplayPriorityAscProductCodeAsc())
                .thenReturn(catalog());
    }

    @Test
    void returnsAllActiveProductsWhenNoFilter() {
        List<ProductResponse> result = service.getActiveProducts(null, null);

        assertThat(result).extracting(ProductResponse::productCode)
                .containsExactly("CLEANSER", "MOISTURIZER", "REDNESS", "WRINKLE");
    }

    @Test
    void filtersByCategory() {
        List<ProductResponse> result = service.getActiveProducts(ProductCategory.TARGETED_CARE, null);

        assertThat(result).extracting(ProductResponse::category)
                .containsOnly(ProductCategory.TARGETED_CARE);
        assertThat(result).extracting(ProductResponse::productCode)
                .containsExactly("REDNESS", "WRINKLE");
    }

    @Test
    void filtersByConcern() {
        List<ProductResponse> result = service.getActiveProducts(null, ProductConcern.REDNESS);

        assertThat(result).extracting(ProductResponse::productCode).containsExactly("REDNESS");
    }

    @Test
    void filtersByCategoryAndConcernTogether() {
        List<ProductResponse> result =
                service.getActiveProducts(ProductCategory.CLEANSER, ProductConcern.REDNESS);

        assertThat(result).isEmpty();
    }

    private List<Product> catalog() {
        return List.of(
                product("CLEANSER", ProductCategory.CLEANSER, ProductConcern.GENERAL, 1),
                product("MOISTURIZER", ProductCategory.MOISTURIZER, ProductConcern.GENERAL, 2),
                product("REDNESS", ProductCategory.TARGETED_CARE, ProductConcern.REDNESS, 3),
                product("WRINKLE", ProductCategory.TARGETED_CARE, ProductConcern.WRINKLE, 4)
        );
    }

    private Product product(String code, ProductCategory category, ProductConcern concern, int priority) {
        return new Product(code, "[데모]", code, category, concern, true, true, true, null, priority);
    }
}
