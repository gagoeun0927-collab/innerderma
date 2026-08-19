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

/**
 * Seeds hackathon demo/dummy data (demo users, facilities, baseline diagnosis,
 * procedures, and products). Runs on all profiles including prod for demo/hackathon purposes.
 * Idempotent — existing data is not overwritten.
 */
@Configuration
public class DemoDataInitializer {

    public static final String DEMO_USER_CODE = "WHS-DEMO-001";
    private static final LocalDate DEMO_DATE = LocalDate.of(2026, 8, 15);

    @Bean
    CommandLineRunner initializeDemoData(
            UserRepository userRepository,
            FacilityRepository facilityRepository,
            WhsSkinDiagnosisRepository diagnosisRepository,
            ProcedureRecordRepository procedureRecordRepository,
            ProductRepository productRepository
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
}
