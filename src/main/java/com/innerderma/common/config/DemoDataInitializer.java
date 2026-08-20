package com.innerderma.common.config;

import com.innerderma.facility.domain.Facility;
import com.innerderma.facility.domain.FacilityRepository;
import com.innerderma.procedure.domain.ProcedureRecord;
import com.innerderma.procedure.domain.ProcedureRecordRepository;
import com.innerderma.product.domain.*;
import com.innerderma.skindiagnosis.domain.WhsSkinDiagnosis;
import com.innerderma.skindiagnosis.domain.WhsSkinDiagnosisRepository;
import com.innerderma.skindiagnosis.domain.SkinDiagnosisGrade;
import com.innerderma.skindiagnosis.domain.SkinDiagnosisMetricType;
import com.innerderma.skindiagnosis.domain.WhsSkinDiagnosisMetric;

import com.innerderma.user.domain.User;
import com.innerderma.user.domain.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Seeds hackathon demo/dummy data (demo users, facilities, baseline diagnosis,
 * procedures, and products). Runs on all profiles including prod for demo/hackathon purposes.
 * Idempotent — existing data is not overwritten.
 */
@Configuration
public class DemoDataInitializer {

    private static final org.slf4j.Logger log =
            org.slf4j.LoggerFactory.getLogger(DemoDataInitializer.class);

    public static final String DEMO_USER_CODE = "WHS-DEMO-001";
    private static final LocalDate DEMO_DATE = LocalDate.of(2026, 8, 15);

    @Bean
    CommandLineRunner initializeDemoData(
            UserRepository userRepository,
            FacilityRepository facilityRepository,
            WhsSkinDiagnosisRepository diagnosisRepository,
            ProcedureRecordRepository procedureRecordRepository,
            ProductRepository productRepository,
            ProductTranslationRepository translationRepository,
            com.innerderma.knowledge.product.PieceSeoulKnowledgeBase pieceSeoulKb,
            com.innerderma.knowledge.product.WimStoreKnowledgeBase wimStoreKb
    ) {
        return args -> {
            User user = userRepository.findByUserCode(DEMO_USER_CODE)
                    .orElseGet(() -> userRepository.save(
                            new User(DEMO_USER_CODE, "테스트 사용자", "010-1234-1234")
                    ));

            getOrCreateFacility(facilityRepository, "WHS", "웰니스 하우스 서울");
            Facility derna = getOrCreateFacility(facilityRepository, "DERNA", "더나클리닉");
            getOrCreateFacility(facilityRepository, "AMRED", "엠레드의원");

            if (!diagnosisRepository.existsByUser_UserCode(DEMO_USER_CODE)) {
                diagnosisRepository.save(new WhsSkinDiagnosis(
                        user,
                        DEMO_DATE,
                        "WHS 피부 진단 결과입니다.",
                        List.of(
                                metric(SkinDiagnosisMetricType.SKIN_AGE, null),
                                metric(SkinDiagnosisMetricType.FOREHEAD_WRINKLE, null),
                                metric(SkinDiagnosisMetricType.CROW_FEET_WRINKLE, null),
                                metric(SkinDiagnosisMetricType.UNDER_EYE_WRINKLE, null),
                                metric(SkinDiagnosisMetricType.PIGMENTATION, SkinDiagnosisGrade.NORMAL),
                                metric(SkinDiagnosisMetricType.SKIN_UNIFORMITY, SkinDiagnosisGrade.EXCELLENT),
                                metric(SkinDiagnosisMetricType.ACNE, SkinDiagnosisGrade.EXCELLENT),
                                metric(SkinDiagnosisMetricType.BLACKHEAD, SkinDiagnosisGrade.NEEDS_IMPROVEMENT),
                                metric(SkinDiagnosisMetricType.DARK_CIRCLE, SkinDiagnosisGrade.EXCELLENT),
                                metric(SkinDiagnosisMetricType.EYE_SAGGING, SkinDiagnosisGrade.EXCELLENT),
                                metric(SkinDiagnosisMetricType.PORE, SkinDiagnosisGrade.NORMAL)
                        )
                ));
            }

            if (!procedureRecordRepository
                    .existsByUser_UserCodeAndFacility_FacilityCodeAndProcedureDate(
                            DEMO_USER_CODE,
                            derna.getFacilityCode(),
                            DEMO_DATE
                    )) {
                procedureRecordRepository.save(new ProcedureRecord(
                        user,
                        derna,
                        DEMO_DATE,
                        "진정 및 피부 장벽 관리",
                        "자극적인 제품을 피하고 보습제를 충분히 사용할 것",
                        "BARRIER_CARE",
                        "피부 장벽 강화",
                        "전체 얼굴",
                        3,
                        7,
                        List.of("약간의 당김감", "일시적 홍조"),
                        List.of("48시간 이상 지속되는 부종", "심한 통증"),
                        List.of("자극적인 제품 사용 금지", "충분한 보습"),
                        List.of("moisturizer", "barrier"),
                        List.of("retinol", "aha", "bha"),
                        "AAC_CLINIC",
                        "1.0.0"
                ));
            }

            initializeDemoProducts(productRepository);
            seedKnowledgeBaseProducts(productRepository, pieceSeoulKb, wimStoreKb);
            seedProductTranslations(translationRepository);
        };
    }

    private WhsSkinDiagnosisMetric metric(SkinDiagnosisMetricType type, SkinDiagnosisGrade grade) {
        // The supplied WHS result contains no numeric source values, so both scores remain null.
        return new WhsSkinDiagnosisMetric(type, null, null, grade);
    }

    private void initializeDemoProducts(ProductRepository repository) {
        createProduct(repository, "DEMO-CLEANSER-001", "[데모] InnerDerma", "순한 클렌저",
                ProductCategory.CLEANSER, ProductConcern.GENERAL, true, 10);
        createProduct(repository, "DEMO-MOISTURIZER-001", "[데모] InnerDerma", "장벽 보습제",
                ProductCategory.MOISTURIZER, ProductConcern.GENERAL, true, 20);
        createProduct(repository, "DEMO-SUNSCREEN-001", "[데모] InnerDerma", "데일리 자외선 차단제",
                ProductCategory.SUNSCREEN, ProductConcern.GENERAL, true, 30);
        createProduct(repository, "DEMO-WRINKLE-001", "[데모] InnerDerma", "주름 집중 케어",
                ProductCategory.TARGETED_CARE, ProductConcern.WRINKLE, false, 40);
        createProduct(repository, "DEMO-PORE-001", "[데모] InnerDerma", "모공·피부결 집중 케어",
                ProductCategory.TARGETED_CARE, ProductConcern.PORE_TEXTURE, false, 41);
        createProduct(repository, "DEMO-PIGMENT-001", "[데모] InnerDerma", "색소 집중 케어",
                ProductCategory.TARGETED_CARE, ProductConcern.PIGMENTATION, false, 42);
        createProduct(repository, "DEMO-REDNESS-001", "[데모] InnerDerma", "홍조 진정 케어",
                ProductCategory.TARGETED_CARE, ProductConcern.REDNESS, false, 43);
    }

    private void createProduct(ProductRepository repository, String code, String brand, String name,
                               ProductCategory category, ProductConcern concern,
                               boolean attentionCompatible, int priority) {
        if (!repository.existsByProductCode(code)) {
            repository.save(new Product(code, brand, name, category, concern,
                    attentionCompatible, true, true, null, priority));
        }
    }

    private Facility getOrCreateFacility(
            FacilityRepository facilityRepository,
            String facilityCode,
            String name
    ) {
        return facilityRepository.findByFacilityCode(facilityCode)
                .orElseGet(() -> facilityRepository.save(new Facility(facilityCode, name)));
    }

    private void seedKnowledgeBaseProducts(ProductRepository repository,
                                         com.innerderma.knowledge.product.PieceSeoulKnowledgeBase pieceSeoulKb,
                                         com.innerderma.knowledge.product.WimStoreKnowledgeBase wimStoreKb) {
        int priority = 100;
        int pieceSeoulSaved = 0;
        for (var p : pieceSeoulKb.findAll()) {
            if (p.productId() == null || p.name() == null) continue;
            if (!repository.existsByProductCode(p.productId())) {
                repository.save(new Product(
                        p.productId(), p.brand() != null ? p.brand() : "Piece Seoul", p.name(),
                        mapCategory(p.category()), ProductConcern.GENERAL, true,
                        true, false, p.officialUrl(), priority++,
                        "PIECE_SEOUL", p.price(), p.imageUrl(), p.frequency(),
                        p.applicationMethod(), toJson(p.verifiedClaims()),
                        toJson(p.ingredientsHighlight()), toJson(p.skinStateTags())
                ));
                pieceSeoulSaved++;
            }
        }

        int wimStoreSaved = 0;
        for (var p : wimStoreKb.findAll()) {
            if (p.productId() == null || p.name() == null) continue;
            if (!repository.existsByProductCode(p.productId())) {
                repository.save(new Product(
                        p.productId(), p.brand() != null ? p.brand() : "WIM Store", p.name(),
                        mapCategory(p.category()), ProductConcern.GENERAL, true,
                        true, false, p.officialUrl(), priority++,
                        "WIM_STORE", p.price(), p.imageUrl(), p.usage(),
                        null, toJson(p.verifiedClaims()),
                        toJson(p.ingredientsHighlight()), toJson(p.stateTags())
                ));
                wimStoreSaved++;
            }
        }

        log.info("KB product seeding done. PieceSeoul: {} newly saved of {} in KB, WimStore: {} newly saved of {} in KB",
                pieceSeoulSaved, pieceSeoulKb.size(), wimStoreSaved, wimStoreKb.size());
    }

    private ProductCategory mapCategory(String kbCategory) {
        if (kbCategory == null) return ProductCategory.TARGETED_CARE;
        return switch (kbCategory.toUpperCase()) {
            case "CLEANSER" -> ProductCategory.CLEANSER;
            case "TONER" -> ProductCategory.TONER;
            case "SERUM" -> ProductCategory.SERUM;
            case "MOISTURIZER" -> ProductCategory.MOISTURIZER;
            case "SUNSCREEN" -> ProductCategory.SUNSCREEN;
            case "OIL" -> ProductCategory.OIL;
            case "MASK" -> ProductCategory.MASK;
            case "POWDER" -> ProductCategory.POWDER;
            case "JELLY" -> ProductCategory.JELLY;
            case "SUPPLEMENT" -> ProductCategory.SUPPLEMENT;
            case "DRINK" -> ProductCategory.DRINK;
            case "FOOD" -> ProductCategory.FOOD;
            default -> ProductCategory.TARGETED_CARE;
        };
    }

    private String toJson(java.util.List<String> list) {
        if (list == null || list.isEmpty()) return "[]";
        try {
            return new tools.jackson.databind.ObjectMapper().writeValueAsString(list);
        } catch (Exception e) {
            return "[]";
        }
    }

    // === i18n 번역 시딩 ===

    private static final String PIECE_SEOUL_I18N_PATH = "knowledge/piece_seoul_products_i18n.json";
    private static final String WIM_STORE_I18N_PATH = "knowledge/wim_store_products_i18n.json";

    private void seedProductTranslations(ProductTranslationRepository translationRepository) {
        int saved = 0;
        saved += seedTranslationsFromFile(translationRepository, PIECE_SEOUL_I18N_PATH);
        saved += seedTranslationsFromFile(translationRepository, WIM_STORE_I18N_PATH);
        log.info("Product translation seeding done. {} translations saved.", saved);
    }

    private int seedTranslationsFromFile(ProductTranslationRepository repo, String resourcePath) {
        int saved = 0;
        try {
            var is = new org.springframework.core.io.ClassPathResource(resourcePath).getInputStream();
            var mapper = new tools.jackson.databind.ObjectMapper()
                    .rebuild()
                    .disable(tools.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                    .build();
            List<I18nProductEntry> entries = mapper.readValue(is,
                    mapper.getTypeFactory().constructCollectionType(List.class, I18nProductEntry.class));

            for (var entry : entries) {
                if (entry.productId == null || entry.translations == null) continue;
                for (var localeEntry : entry.translations.entrySet()) {
                    String locale = localeEntry.getKey();
                    I18nTranslation t = localeEntry.getValue();
                    if (t == null || t.name == null) continue;
                    if (!repo.existsByProductCodeAndLocale(entry.productId, locale)) {
                        String featuresJson = toJson(t.features);
                        repo.save(new ProductTranslation(
                                entry.productId, locale, t.name, t.usage, featuresJson, t.caution
                        ));
                        saved++;
                    }
                }
            }
        } catch (Exception e) {
            log.error("Failed to seed translations from {}: {}", resourcePath, e.getMessage());
        }
        return saved;
    }

    @com.fasterxml.jackson.annotation.JsonAutoDetect(
            fieldVisibility = com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.ANY)
    private static class I18nProductEntry {
        @com.fasterxml.jackson.annotation.JsonProperty("product_id") String productId;
        java.util.Map<String, I18nTranslation> translations;
    }

    @com.fasterxml.jackson.annotation.JsonAutoDetect(
            fieldVisibility = com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.ANY)
    private static class I18nTranslation {
        String name;
        String usage;
        List<String> features;
        String caution;
    }
}
