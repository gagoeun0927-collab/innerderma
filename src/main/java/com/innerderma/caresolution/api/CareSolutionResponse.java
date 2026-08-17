package com.innerderma.caresolution.api;

import com.innerderma.caresolution.application.CareSolutionResult;
import com.innerderma.caresolution.domain.CareSeason;
import com.innerderma.caresolution.domain.SafetyLevel;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record CareSolutionResponse(
        Long id, Long careCycleId, LocalDate originCaptureDate, LocalDate servedDate,
        boolean inherited, CareSeason season, SafetyLevel safetyLevel, String headline,
        String primaryConcern, List<String> eveningSteps, List<String> morningSteps,
        String safetyMessage, String whsDiagnosisSummary, String procedureName,
        String procedureCareGuide, LocalDateTime generatedAt
) {
    public static CareSolutionResponse from(CareSolutionResult result) {
        var solution = result.solution();
        var diagnosis = solution.getWhsDiagnosis();
        var procedure = solution.getProcedureRecord();
        return new CareSolutionResponse(solution.getId(), solution.getCareCycle().getId(),
                solution.getCareCycle().getOriginCaptureDate(), result.servedDate(), result.inherited(),
                solution.getSeason(), solution.getSafetyLevel(), solution.getHeadline(),
                solution.getPrimaryConcern(), result.eveningSteps(), result.morningSteps(),
                solution.getSafetyMessage(), diagnosis == null ? null : diagnosis.getResultSummary(),
                procedure == null ? null : procedure.getProcedureName(),
                procedure == null ? null : procedure.getCareGuide(), solution.getGeneratedAt());
    }
}
