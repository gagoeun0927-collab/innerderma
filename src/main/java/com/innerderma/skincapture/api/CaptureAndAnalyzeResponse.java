package com.innerderma.skincapture.api;

import com.innerderma.skinanalysis.application.SkinAnalysisResult;
import com.innerderma.skincapture.domain.SkinCapture;

public record CaptureAndAnalyzeResponse(
        Long captureId,
        String qualityStatus,
        boolean analyzed,
        Double overallScore,
        String skinHealthGrade,
        String modelVersion
) {
    public static CaptureAndAnalyzeResponse qualityFailed(SkinCapture capture) {
        return new CaptureAndAnalyzeResponse(
                capture.getId(),
                capture.getQualityStatus().name(),
                false, null, null, null
        );
    }

    public static CaptureAndAnalyzeResponse success(SkinCapture capture, SkinAnalysisResult result) {
        return new CaptureAndAnalyzeResponse(
                capture.getId(),
                capture.getQualityStatus().name(),
                true,
                result.analysis().getOverallScore(),
                result.analysis().getSkinHealthGrade(),
                result.analysis().getModelVersion()
        );
    }
}
