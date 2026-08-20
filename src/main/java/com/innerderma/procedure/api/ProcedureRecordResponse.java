package com.innerderma.procedure.api;

import com.innerderma.procedure.domain.ProcedureRecord;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.util.List;

/**
 * 시술 기록 응답.
 *
 * <p>회복 기간·증상·주의사항·제품 태그는 Treatment Knowledge Base에서 채워진 값이며
 * 클라이언트 입력이 아니다. 프론트가 등록 결과를 확인하고 회복기 안내를 표시할 수 있도록 노출한다.
 */
@Schema(description = "시술 기록")
public record ProcedureRecordResponse(
        Long id,
        String facilityCode,
        String facilityName,
        LocalDate procedureDate,
        @Schema(description = "시술명", example = "레이저 토닝")
        String procedureName,
        @Schema(description = "시술 후 케어 가이드")
        String careGuide,
        @Schema(description = "시술 코드", example = "LASER_TONING")
        String treatmentCode,
        @Schema(description = "시술 유형", example = "LASER_TONING")
        String treatmentType,
        @Schema(description = "시술 부위")
        String treatmentArea,
        @Schema(description = "예상 회복 기간 최소일", example = "2")
        Integer expectedRecoveryDaysMin,
        @Schema(description = "예상 회복 기간 최대일", example = "5")
        Integer expectedRecoveryDaysMax,
        @Schema(description = "정상적으로 나타날 수 있는 증상")
        List<String> normalSymptoms,
        @Schema(description = "즉시 확인이 필요한 경고 증상")
        List<String> warningSymptoms,
        @Schema(description = "시술 후 주의사항")
        List<String> aftercareRestrictions,
        @Schema(description = "사용 권장 제품 태그")
        List<String> allowedProductTags,
        @Schema(description = "사용 제한 제품 태그")
        List<String> restrictedProductTags
) {
    public static ProcedureRecordResponse from(ProcedureRecord record) {
        return new ProcedureRecordResponse(
                record.getId(),
                record.getFacility().getFacilityCode(),
                record.getFacility().getName(),
                record.getProcedureDate(),
                record.getProcedureName(),
                record.getCareGuide(),
                record.getTreatmentCode(),
                record.getTreatmentType(),
                record.getTreatmentArea(),
                record.getExpectedRecoveryDaysMin(),
                record.getExpectedRecoveryDaysMax(),
                record.getNormalSymptoms(),
                record.getWarningSymptoms(),
                record.getAftercareRestrictions(),
                record.getAllowedProductTags(),
                record.getRestrictedProductTags()
        );
    }
}
