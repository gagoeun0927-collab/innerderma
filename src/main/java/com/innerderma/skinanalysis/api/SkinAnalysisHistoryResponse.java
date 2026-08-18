package com.innerderma.skinanalysis.api;

import com.innerderma.skinanalysis.application.SkinAnalysisHistoryResult;
import com.innerderma.skinanalysis.domain.SkinAnalysis;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record SkinAnalysisHistoryResponse(LocalDate from, LocalDate to, int count, List<Item> items) {

    public record Item(
            Long id,
            Long captureId,
            LocalDateTime analyzedAt,
            double overallScore,
            String skinHealthGrade,
            String modelVersion
    ) {
        static Item from(SkinAnalysis analysis) {
            return new Item(
                    analysis.getId(),
                    analysis.getSkinCapture().getId(),
                    analysis.getAnalyzedAt(),
                    analysis.getOverallScore(),
                    analysis.getSkinHealthGrade(),
                    analysis.getModelVersion()
            );
        }
    }

    public static SkinAnalysisHistoryResponse from(SkinAnalysisHistoryResult result) {
        List<Item> items = result.items().stream().map(Item::from).toList();
        return new SkinAnalysisHistoryResponse(result.from(), result.to(), items.size(), items);
    }
}
