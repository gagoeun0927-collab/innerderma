package com.innerderma.procedure.api;

import com.innerderma.procedure.application.TreatmentContext;

import java.time.LocalDate;
import java.util.List;

public record TreatmentContextResponse(
        Long treatmentId,
        String treatmentCode,
        String treatmentType,
        LocalDate treatmentDate,
        String treatmentArea,
        long daysSinceTreatment,
        Integer expectedRecoveryDaysMin,
        Integer expectedRecoveryDaysMax,
        List<String> normalSymptoms,
        List<String> warningSymptoms,
        List<String> aftercareRestrictions,
        List<String> allowedProductTags,
        List<String> restrictedProductTags,
        String source,
        String ruleVersion
) {
    public static TreatmentContextResponse from(TreatmentContext context) {
        return new TreatmentContextResponse(
                context.treatmentId(), context.treatmentCode(), context.treatmentType(), context.treatmentDate(),
                context.treatmentArea(), context.daysSinceTreatment(), context.expectedRecoveryDaysMin(),
                context.expectedRecoveryDaysMax(), context.normalSymptoms(), context.warningSymptoms(),
                context.aftercareRestrictions(), context.allowedProductTags(), context.restrictedProductTags(),
                context.source(), context.ruleVersion());
    }
}
