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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class ProcedureRecordService {

    /** 시술 기관을 지정하지 않았을 때 사용할 기본 기관 */
    private static final String DEFAULT_FACILITY_CODE = "WHS";

    private final ProcedureRecordRepository procedureRecordRepository;
    private final UserRepository userRepository;
    private final FacilityRepository facilityRepository;
    private final TreatmentKnowledgeBase treatmentKnowledgeBase;
    private final SolutionCache solutionCache;

    public ProcedureRecordService(ProcedureRecordRepository procedureRecordRepository,
                                  UserRepository userRepository,
                                  FacilityRepository facilityRepository,
                                  TreatmentKnowledgeBase treatmentKnowledgeBase,
                                  SolutionCache solutionCache) {
        this.procedureRecordRepository = procedureRecordRepository;
        this.userRepository = userRepository;
        this.facilityRepository = facilityRepository;
        this.treatmentKnowledgeBase = treatmentKnowledgeBase;
        this.solutionCache = solutionCache;
    }

    /**
     * 시술 여부를 등록한다.
     *
     * <p>회복 기간·정상/경고 증상·주의사항·제품 태그는 Treatment KB의 값을 그대로 사용한다.
     * 클라이언트가 보낸 값으로 임상 정보를 채우지 않으며, KB에 없는 시술 코드는 거부한다.
     *
     * @return 생성된 기록. hadProcedure=false면 null (기록을 만들지 않음)
     */
    @Transactional
    public ProcedureRecord register(String userCode, boolean hadProcedure, String treatmentCode,
                                   LocalDate procedureDate, String facilityCode) {
        User user = userRepository.findByUserCode(userCode)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        // "시술 안 받음"은 기록을 만들지 않는다. 시술 기록이 없는 상태가 곧 미시술 상태다.
        if (!hadProcedure) {
            solutionCache.invalidate(userCode);
            return null;
        }

        if (treatmentCode == null || treatmentCode.isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }
        TreatmentRule rule = treatmentKnowledgeBase.findByCode(treatmentCode)
                .orElseThrow(() -> new BusinessException(ErrorCode.TREATMENT_NOT_FOUND));

        LocalDate resolvedDate = procedureDate == null ? LocalDate.now() : procedureDate;
        String resolvedFacilityCode = (facilityCode == null || facilityCode.isBlank())
                ? DEFAULT_FACILITY_CODE : facilityCode;
        Facility facility = facilityRepository.findByFacilityCode(resolvedFacilityCode)
                .orElseThrow(() -> new BusinessException(ErrorCode.FACILITY_NOT_FOUND));

        // 같은 기관·같은 날짜에 이미 기록이 있으면 중복 생성하지 않는다.
        if (procedureRecordRepository.existsByUser_UserCodeAndFacility_FacilityCodeAndProcedureDate(
                userCode, resolvedFacilityCode, resolvedDate)) {
            throw new BusinessException(ErrorCode.PROCEDURE_ALREADY_EXISTS);
        }

        ProcedureRecord record = new ProcedureRecord(
                user,
                facility,
                resolvedDate,
                rule.treatmentName(),
                rule.aftercareGuide(),
                rule.treatmentCode(),
                rule.treatmentType(),
                rule.treatmentArea().isEmpty() ? null : String.join(",", rule.treatmentArea()),
                rule.expectedRecoveryDaysMin(),
                rule.expectedRecoveryDaysMax(),
                rule.normalSymptoms(),
                rule.warningSymptoms(),
                rule.aftercareRestrictions(),
                rule.allowedProductTags(),
                rule.restrictedProductTags(),
                rule.source(),
                rule.version()
        );
        ProcedureRecord saved = procedureRecordRepository.save(record);

        // 시술 정보가 바뀌면 기존 Solution은 유효하지 않다 (§35 재생성 조건)
        solutionCache.invalidate(userCode);
        return saved;
    }

    /** 등록 가능한 시술 목록 (프론트 선택 UI용) */
    public List<TreatmentRule> getAvailableTreatments() {
        return treatmentKnowledgeBase.findAll();
    }

    public List<ProcedureRecord> getProcedureRecords(
            String userCode,
            String facilityCode,
            LocalDate procedureDate
    ) {
        List<ProcedureRecord> records = procedureRecordRepository
                .findAllByUser_UserCodeAndFacility_FacilityCodeAndProcedureDate(
                        userCode,
                        facilityCode,
                        procedureDate
                );

        if (records.isEmpty()) {
            throw new BusinessException(ErrorCode.PROCEDURE_NOT_FOUND);
        }
        return records;
    }

    public TreatmentContext getTreatmentContext(String userCode, LocalDate referenceDate) {
        ProcedureRecord record = procedureRecordRepository
                .findFirstByUser_UserCodeAndProcedureDateLessThanEqualOrderByProcedureDateDescIdDesc(
                        userCode, referenceDate)
                .orElseThrow(() -> new BusinessException(ErrorCode.PROCEDURE_NOT_FOUND));
        return TreatmentContext.from(record, referenceDate);
    }

    public ProcedureRecord getProcedureRecord(String userCode, Long id) {
        return procedureRecordRepository.findByIdAndUser_UserCode(id, userCode)
                .orElseThrow(() -> new BusinessException(ErrorCode.PROCEDURE_NOT_FOUND));
    }
}
