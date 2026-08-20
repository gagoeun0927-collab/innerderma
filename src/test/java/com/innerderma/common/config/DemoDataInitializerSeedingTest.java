package com.innerderma.common.config;

import com.innerderma.knowledge.product.PieceSeoulKnowledgeBase;
import com.innerderma.knowledge.product.WimStoreKnowledgeBase;
import com.innerderma.product.domain.Product;
import com.innerderma.product.domain.ProductCategory;
import com.innerderma.product.domain.ProductRepository;
import com.innerderma.product.domain.ProductTranslation;
import com.innerderma.product.domain.ProductTranslationRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * KB(JSON) → products 테이블 시딩이 애플리케이션 기동 시 실제로 수행되는지 검증한다.
 * 회귀 방지 대상: Jackson이 package-private 필드를 못 읽어 name이 null이 되면
 * DemoDataInitializer가 모든 항목을 조용히 스킵하고 시딩이 0건이 된다.
 */
@SpringBootTest
class DemoDataInitializerSeedingTest {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ProductTranslationRepository translationRepository;

    @Autowired
    private PieceSeoulKnowledgeBase pieceSeoulKb;

    @Autowired
    private WimStoreKnowledgeBase wimStoreKb;

    @Test
    void knowledgeBasesAreLoadedInSpringContext() {
        assertThat(pieceSeoulKb.size()).isEqualTo(22);
        assertThat(wimStoreKb.size()).isEqualTo(16);
    }

    @Test
    void seedsAllPieceSeoulProducts() {
        List<Product> saved = productRepository.findAll().stream()
                .filter(p -> "PIECE_SEOUL".equals(p.getSource()))
                .toList();

        assertThat(saved).hasSize(22);
        assertThat(saved).allSatisfy(p -> {
            assertThat(p.getName()).isNotBlank();
            assertThat(p.getBrand()).isNotBlank();
            assertThat(p.getPrice()).isNotNull().isPositive();
            assertThat(p.getOfficialUrl()).isNotBlank();
            assertThat(p.isDemoProduct()).isFalse();
            assertThat(p.getImageUrl()).startsWith("https://inner-derma.duckdns.org/product-images/");
            assertThat(p.getImageUrl()).endsWith(".jpg");
        });
    }

    @Test
    void seedsAllWimStoreProducts() {
        List<Product> saved = productRepository.findAll().stream()
                .filter(p -> "WIM_STORE".equals(p.getSource()))
                .toList();

        assertThat(saved).hasSize(16);
        assertThat(saved).allSatisfy(p -> {
            assertThat(p.getName()).isNotBlank();
            assertThat(p.getPrice()).isNotNull().isPositive();
            assertThat(p.isDemoProduct()).isFalse();
        });
    }

    @Test
    void mapsWimStoreIngestibleCategories() {
        List<ProductCategory> categories = productRepository.findAll().stream()
                .filter(p -> "WIM_STORE".equals(p.getSource()))
                .map(Product::getCategory)
                .distinct()
                .toList();

        // 섭취류는 TARGETED_CARE로 뭉개지지 않고 확장된 enum으로 매핑돼야 한다.
        assertThat(categories).contains(ProductCategory.POWDER);
        assertThat(categories).doesNotContain(ProductCategory.CLEANSER);
    }

    @Test
    void keepsDemoProductsSeparateFromKbProducts() {
        List<Product> demo = productRepository.findAll().stream()
                .filter(Product::isDemoProduct)
                .toList();

        assertThat(demo).isNotEmpty();
        assertThat(demo).allSatisfy(p -> assertThat(p.getSource()).isNull());
    }

    @Test
    void seedsTranslationsForAllKbProducts() {
        // (30 piece_seoul + 18 wim_store) × 4 locales (ko/en/ja/zh) = 192 translations
        List<ProductTranslation> all = translationRepository.findAll();
        assertThat(all).hasSize(192);
    }

    @Test
    void seedsEnglishTranslationForPieceSeoulProduct() {
        var tr = translationRepository.findByProductCodeAndLocale("PSS_001", "en");
        assertThat(tr).isPresent();
        assertThat(tr.get().getName()).isEqualTo("Core Rebuild Cream");
        assertThat(tr.get().getUsage()).isNotBlank();
        assertThat(tr.get().getFeaturesJson()).contains("beta-sitosterol");
        assertThat(tr.get().getCaution()).isNotBlank();
    }

    @Test
    void seedsJapaneseTranslationForWimStoreProduct() {
        var tr = translationRepository.findByProductCodeAndLocale("WIM_001", "ja");
        assertThat(tr).isPresent();
        assertThat(tr.get().getName()).contains("プロテインシェイク");
        assertThat(tr.get().getLocale()).isEqualTo("ja");
    }

    @Test
    void seedsChineseTranslationExists() {
        var tr = translationRepository.findByProductCodeAndLocale("PSS_010", "zh");
        assertThat(tr).isPresent();
        assertThat(tr.get().getLocale()).isEqualTo("zh");
        assertThat(tr.get().getName()).isNotBlank();
    }
}
