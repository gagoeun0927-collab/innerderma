package com.innerderma.skinanalysis.api;

import com.innerderma.skinanalysis.application.SkinAgeAnalysisResult;
import com.innerderma.skinanalysis.application.SkinAnalysisResult;

import java.time.LocalDateTime;

public record SkinAnalysisResponse(
        Long id,
        Long captureId,
        String userCode,
        LocalDateTime analyzedAt,
        SkinAgeAnalysisResult result
) {
    public static SkinAnalysisResponse from(SkinAnalysisResult analysisResult) {
        var analysis = analysisResult.analysis();
        var capture = analysis.getSkinCapture();
        return new SkinAnalysisResponse(
                analysis.getId(),
                capture.getId(),
                capture.getUser().getUserCode(),
                analysis.getAnalyzedAt(),
                analysisResult.result()
        );
    }
}
