package com.innerderma.procedure.api;

import com.innerderma.knowledge.treatment.TreatmentRule;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * 시술 선택 UI용 시술 정보.
 * Treatment KB의 값을 그대로 노출하며, 회복 기간은 참고용으로만 제공한다.
 */
@Schema(description = "등록 가능한 시술 정보")
public record AvailableTreatmentResponse(
        @Schema(description = "시술 코드. 등록 시 이 값을 treatmentCode로 보냅니다.", example = "laser_toning")
        String treatmentCode,
        @Schema(description = "시술명 (한국어)", example = "레이저 토닝")
        String treatmentName,
        @Schema(description = "시술명 (영어)", example = "Laser Toning")
        String treatmentNameEn,
        @Schema(description = "시술 유형", example = "laser")
        String treatmentType,
        @Schema(description = "시술 부위")
        List<String> treatmentArea,
        @Schema(description = "예상 회복 기간 최소일", example = "3")
        int expectedRecoveryDaysMin,
        @Schema(description = "예상 회복 기간 최대일", example = "7")
        int expectedRecoveryDaysMax
) {
    public static AvailableTreatmentResponse from(TreatmentRule rule) {
        return new AvailableTreatmentResponse(
                rule.treatmentCode(),
                rule.treatmentName(),
                rule.treatmentNameEn(),
                rule.treatmentType(),
                rule.treatmentArea(),
                rule.expectedRecoveryDaysMin(),
                rule.expectedRecoveryDaysMax()
        );
    }
}
