package com.innerderma.procedure.application;

import com.innerderma.airule.cache.SolutionCache;
import com.innerderma.common.error.BusinessException;
import com.innerderma.common.error.ErrorCode;
import com.innerderma.facility.domain.Facility;
import com.innerderma.facility.domain.FacilityRepository;
import com.innerderma.knowledge.treatment.TreatmentKnowledgeBase;
import com.innerderma.knowledge.treatment.TreatmentRule;
import com.innerderma.procedure.domain.ProcedureRecord;
import com.innerderma.procedure.domain.ProcedureRecordRepository;
import com.innerderma.user.domain.User;
import com.innerderma.user.domain.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 시술 여부 등록 검증.
 *
 * <p>핵심 원칙: 회복 기간·증상·주의사항 같은 임상 값은 클라이언트 입력이 아니라
 * Treatment KB에서 가져와야 한다. 근거 없는 임상 값이 저장되지 않는지 검증한다.
 */
class ProcedureRegisterServiceTest {

    private static final String USER_CODE = "WHS-DEMO-001";

    private ProcedureRecordRepository procedureRepository;
    private UserRepository userRepository;
    private FacilityRepository facilityRepository;
    private TreatmentKnowledgeBase treatmentKb;
    private SolutionCache solutionCache;
    private ProcedureRecordService service;

    @BeforeEach
    void setUp() {
        procedureRepository = mock(ProcedureRecordRepository.class);
        userRepository = mock(UserRepository.class);
        facilityRepository = mock(FacilityRepository.class);
        treatmentKb = mock(TreatmentKnowledgeBase.class);
        solutionCache = mock(SolutionCache.class);
        service = new ProcedureRecordService(procedureRepository, userRepository,
                facilityRepository, treatmentKb, solutionCache);

        when(userRepository.findByUserCode(USER_CODE))
                .thenReturn(Optional.of(new User(USER_CODE, "테스트", "010-1234-1234")));
        when(facilityRepository.findByFacilityCode("WHS"))
                .thenReturn(Optional.of(new Facility("WHS", "웰니스 하우스 서울")));
        when(procedureRepository.save(any(ProcedureRecord.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    private TreatmentRule laserToning() {
        return new TreatmentRule("laser_toning", "레이저 토닝", "Laser Toning", "laser",
                List.of("full_face"), 3, 7,
                List.of("일시적 홍조"), List.of("48시간 이상 부종"),
                List.of("자극적인 제품 사용 금지"),
                List.of("moisturizer", "barrier"), List.of("retinol", "aha"),
                "시술 후 48시간은 자극을 최소화하세요.", "AAC_CLINIC", "1.0.0");
    }

    @Test
    void fillsClinicalFieldsFromTreatmentKnowledgeBase() {
        when(treatmentKb.findByCode("laser_toning")).thenReturn(Optional.of(laserToning()));

        ProcedureRecord saved = service.register(USER_CODE, true, "laser_toning",
                LocalDate.of(2026, 8, 20), "WHS");

        assertThat(saved).isNotNull();
        assertThat(saved.getTreatmentCode()).isEqualTo("laser_toning");
        assertThat(saved.getProcedureName()).isEqualTo("레이저 토닝");
        assertThat(saved.getTreatmentType()).isEqualTo("laser");
        // 임상 값은 KB에서 온 것이어야 한다
        assertThat(saved.getExpectedRecoveryDaysMin()).isEqualTo(3);
        assertThat(saved.getExpectedRecoveryDaysMax()).isEqualTo(7);
        assertThat(saved.getNormalSymptoms()).containsExactly("일시적 홍조");
        assertThat(saved.getWarningSymptoms()).containsExactly("48시간 이상 부종");
        assertThat(saved.getAftercareRestrictions()).containsExactly("자극적인 제품 사용 금지");
        assertThat(saved.getRestrictedProductTags()).containsExactly("retinol", "aha");
        assertThat(saved.getProcedureDate()).isEqualTo(LocalDate.of(2026, 8, 20));
    }

    @Test
    void doesNotCreateRecordWhenNoProcedure() {
        ProcedureRecord saved = service.register(USER_CODE, false, null, null, null);

        assertThat(saved).isNull();
        verify(procedureRepository, never()).save(any());
        // 미시술로 바뀐 것도 추천에 영향을 주므로 캐시는 무효화해야 한다
        verify(solutionCache).invalidate(USER_CODE);
    }

    @Test
    void invalidatesSolutionCacheAfterRegistration() {
        when(treatmentKb.findByCode("laser_toning")).thenReturn(Optional.of(laserToning()));

        service.register(USER_CODE, true, "laser_toning", LocalDate.of(2026, 8, 20), "WHS");

        verify(solutionCache).invalidate(USER_CODE);
    }

    @Test
    void rejectsUnknownTreatmentCode() {
        when(treatmentKb.findByCode("unknown_code")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.register(USER_CODE, true, "unknown_code", null, null))
                .isInstanceOfSatisfying(BusinessException.class, e ->
                        assertThat(e.errorCode()).isEqualTo(ErrorCode.TREATMENT_NOT_FOUND));
        verify(procedureRepository, never()).save(any());
    }

    @Test
    void rejectsMissingTreatmentCodeWhenHadProcedure() {
        assertThatThrownBy(() -> service.register(USER_CODE, true, null, null, null))
                .isInstanceOfSatisfying(BusinessException.class, e ->
                        assertThat(e.errorCode()).isEqualTo(ErrorCode.INVALID_REQUEST));
        verify(procedureRepository, never()).save(any());
    }

    @Test
    void rejectsUnknownUser() {
        when(userRepository.findByUserCode("NO-SUCH-USER")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.register("NO-SUCH-USER", true, "laser_toning", null, null))
                .isInstanceOfSatisfying(BusinessException.class, e ->
                        assertThat(e.errorCode()).isEqualTo(ErrorCode.USER_NOT_FOUND));
    }

    @Test
    void rejectsDuplicateProcedureOnSameFacilityAndDate() {
        when(treatmentKb.findByCode("laser_toning")).thenReturn(Optional.of(laserToning()));
        when(procedureRepository.existsByUser_UserCodeAndFacility_FacilityCodeAndProcedureDate(
                USER_CODE, "WHS", LocalDate.of(2026, 8, 20))).thenReturn(true);

        assertThatThrownBy(() -> service.register(USER_CODE, true, "laser_toning",
                LocalDate.of(2026, 8, 20), "WHS"))
                .isInstanceOfSatisfying(BusinessException.class, e ->
                        assertThat(e.errorCode()).isEqualTo(ErrorCode.PROCEDURE_ALREADY_EXISTS));
    }

    @Test
    void defaultsToTodayAndDefaultFacilityWhenOmitted() {
        when(treatmentKb.findByCode("laser_toning")).thenReturn(Optional.of(laserToning()));

        ProcedureRecord saved = service.register(USER_CODE, true, "laser_toning", null, null);

        assertThat(saved.getProcedureDate()).isEqualTo(LocalDate.now());
        assertThat(saved.getFacility().getFacilityCode()).isEqualTo("WHS");
    }
}
