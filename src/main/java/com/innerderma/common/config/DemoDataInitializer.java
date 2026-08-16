package com.innerderma.common.config;

import com.innerderma.facility.domain.Facility;
import com.innerderma.facility.domain.FacilityRepository;
import com.innerderma.procedure.domain.ProcedureRecord;
import com.innerderma.procedure.domain.ProcedureRecordRepository;
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
            ProcedureRecordRepository procedureRecordRepository
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
        };
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
