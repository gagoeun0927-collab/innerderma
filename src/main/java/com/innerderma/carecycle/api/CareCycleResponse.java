package com.innerderma.carecycle.api;

import com.innerderma.carecycle.application.CareCycleResult;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record CareCycleResponse(Long id, Long skinAnalysisId, Long skinCaptureId, Long selfCheckId,
                                LocalDate originCaptureDate, LocalDate eveningCareDate,
                                LocalDate morningCareDate, LocalDate servedDate,
                                boolean inherited, boolean requiresSafetyAttention,
                                LocalDateTime createdAt) {
    public static CareCycleResponse from(CareCycleResult result) {
        var cycle = result.careCycle();
        var selfCheck = cycle.getSelfCheck();
        return new CareCycleResponse(cycle.getId(), cycle.getSkinAnalysis().getId(),
                cycle.getSkinAnalysis().getSkinCapture().getId(),
                selfCheck == null ? null : selfCheck.getId(), cycle.getOriginCaptureDate(),
                cycle.getEveningCareDate(), cycle.getMorningCareDate(), result.servedDate(),
                result.inherited(), selfCheck != null && selfCheck.requiresSafetyAttention(),
                cycle.getCreatedAt());
    }
}
