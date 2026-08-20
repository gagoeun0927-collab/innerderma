package com.innerderma.product.application;

import com.innerderma.common.error.BusinessException;
import com.innerderma.common.error.ErrorCode;
import com.innerderma.product.api.ProductResponse;
import com.innerderma.product.domain.Product;
import com.innerderma.product.domain.ProductCategory;
import com.innerderma.product.domain.ProductConcern;
import com.innerderma.product.domain.ProductRepository;
import com.innerderma.product.domain.ProductTranslation;
import com.innerderma.product.domain.ProductTranslationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ProductServiceTest {

    private ProductRepository repository;
    private ProductTranslationRepository translationRepository;
    private ProductService service;

    @BeforeEach
    void setUp() {
        repository = mock(ProductRepository.class);
        translationRepository = mock(ProductTranslationRepository.class);
        service = new ProductService(repository, translationRepository);
        when(repository.findAllByActiveTrueOrderByDisplayPriorityAscProductCodeAsc())
                .thenReturn(catalog());
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

    @Test
    void returnsTranslationWhenLocaleSpecified() {
        Product product = new Product("PRD-001", "이너덤", "수분 크림",
                ProductCategory.MOISTURIZER, ProductConcern.GENERAL,
                true, true, false, "https://example.com/prd-001", 1);
        when(repository.findByProductCode("PRD-001")).thenReturn(Optional.of(product));
        ProductTranslation tr = new ProductTranslation(
                "PRD-001", "en", "Moisture Cream", "Apply daily",
                "[\"Hydrates skin\"]", "For external use only.");
        when(translationRepository.findByProductCodeAndLocale("PRD-001", "en"))
                .thenReturn(Optional.of(tr));

        ProductResponse result = service.getProduct("PRD-001", "en");

        assertThat(result.translation()).isNotNull();
        assertThat(result.translation().locale()).isEqualTo("en");
        assertThat(result.translation().name()).isEqualTo("Moisture Cream");
        assertThat(result.translation().features()).containsExactly("Hydrates skin");
    }

    @Test
    void returnsNoTranslationWhenLocaleIsKo() {
        Product product = new Product("PRD-001", "이너덤", "수분 크림",
                ProductCategory.MOISTURIZER, ProductConcern.GENERAL,
                true, true, false, "https://example.com/prd-001", 1);
        when(repository.findByProductCode("PRD-001")).thenReturn(Optional.of(product));

        ProductResponse result = service.getProduct("PRD-001", "ko");

        assertThat(result.translation()).isNull();
    }

    @Test
    void normalizesLocaleFromAcceptLanguageFormat() {
        Product product = new Product("PRD-001", "이너덤", "수분 크림",
                ProductCategory.MOISTURIZER, ProductConcern.GENERAL,
                true, true, false, "https://example.com/prd-001", 1);
        when(repository.findByProductCode("PRD-001")).thenReturn(Optional.of(product));
        ProductTranslation tr = new ProductTranslation(
                "PRD-001", "ja", "モイスチャークリーム", "毎日塗布",
                "[\"肌を保湿\"]", "外用専用。");
        when(translationRepository.findByProductCodeAndLocale("PRD-001", "ja"))
                .thenReturn(Optional.of(tr));

        ProductResponse result = service.getProduct("PRD-001", "ja-JP");

        assertThat(result.translation()).isNotNull();
        assertThat(result.translation().locale()).isEqualTo("ja");
    }

    @Test
    void listEndpointIncludesTranslationsForLocale() {
        ProductTranslation tr = new ProductTranslation(
                "CLEANSER", "en", "Gentle Cleanser", "Use daily", "[]", "None.");
        when(translationRepository.findAllByProductCodeInAndLocale(anyList(), eq("en")))
                .thenReturn(List.of(tr));

        List<ProductResponse> result = service.getActiveProducts(null, null, null, "en");

        ProductResponse cleanser = result.stream()
                .filter(p -> "CLEANSER".equals(p.productCode())).findFirst().orElseThrow();
        assertThat(cleanser.translation()).isNotNull();
        assertThat(cleanser.translation().name()).isEqualTo("Gentle Cleanser");

        // Products without translation should have null translation
        ProductResponse moisturizer = result.stream()
                .filter(p -> "MOISTURIZER".equals(p.productCode())).findFirst().orElseThrow();
        assertThat(moisturizer.translation()).isNull();
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
