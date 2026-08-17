package com.innerderma.procedure.application;

import com.innerderma.procedure.domain.ProcedureRecord;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

/** A compact, verified-data-only view of the latest procedure applicable on a reference date. */
public record TreatmentContext(
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
    public static TreatmentContext from(ProcedureRecord record, LocalDate referenceDate) {
        return new TreatmentContext(
                record.getId(), record.getTreatmentCode(), record.getTreatmentType(), record.getProcedureDate(),
                record.getTreatmentArea(), ChronoUnit.DAYS.between(record.getProcedureDate(), referenceDate),
                record.getExpectedRecoveryDaysMin(), record.getExpectedRecoveryDaysMax(),
                record.getNormalSymptoms(), record.getWarningSymptoms(), record.getAftercareRestrictions(),
                record.getAllowedProductTags(), record.getRestrictedProductTags(), record.getSource(),
                record.getRuleVersion()
        );
    }
}
