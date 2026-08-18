package com.innerderma.skindiagnosis.api;

import com.innerderma.skindiagnosis.application.WhsSkinDiagnosisHistoryResult;

import java.time.LocalDate;
import java.util.List;

public record WhsSkinDiagnosisHistoryResponse(LocalDate from, LocalDate to, int count,
                                              List<WhsSkinDiagnosisResponse> items) {
    public static WhsSkinDiagnosisHistoryResponse from(WhsSkinDiagnosisHistoryResult result) {
        List<WhsSkinDiagnosisResponse> items = result.items().stream()
                .map(WhsSkinDiagnosisResponse::from)
                .toList();
        return new WhsSkinDiagnosisHistoryResponse(result.from(), result.to(), items.size(), items);
    }
}
