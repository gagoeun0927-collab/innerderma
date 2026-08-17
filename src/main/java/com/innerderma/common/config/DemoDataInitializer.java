package com.innerderma.common.config;

import com.innerderma.facility.domain.Facility;
import com.innerderma.facility.domain.FacilityRepository;
import com.innerderma.procedure.domain.ProcedureRecord;
import com.innerderma.procedure.domain.ProcedureRecordRepository;
import com.innerderma.product.domain.*;
import com.innerderma.skindiagnosis.domain.WhsSkinDiagnosis;
import com.innerderma.skindiagnosis.domain.WhsSkinDiagnosisRepository;
import com.innerderma.user.domain.User;
import com.innerderma.user.domain.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDate;

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
                        "수분이 부족하고 볼 주변 홍조와 거친 피부결이 관찰됨"
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
                        "자극적인 제품을 피하고 보습제를 충분히 사용할 것"
                ));
            }

            initializeDemoProducts(productRepository);
        };
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
