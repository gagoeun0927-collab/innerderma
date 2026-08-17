package com.innerderma.skindiagnosis.api;

import com.innerderma.skindiagnosis.domain.WhsSkinDiagnosis;

import java.time.LocalDate;
import java.util.List;

public record WhsSkinDiagnosisResponse(
        Long id,
        String userCode,
        LocalDate diagnosedDate,
        String resultSummary,
        List<WhsSkinDiagnosisMetricResponse> metrics
) {
    public static WhsSkinDiagnosisResponse from(WhsSkinDiagnosis diagnosis) {
        return new WhsSkinDiagnosisResponse(
                diagnosis.getId(),
                diagnosis.getUser().getUserCode(),
                diagnosis.getDiagnosedDate(),
                diagnosis.getResultSummary(),
                diagnosis.getMetrics().stream().map(WhsSkinDiagnosisMetricResponse::from).toList()
        );
    }
}
