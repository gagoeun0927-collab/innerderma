package com.innerderma.skinanalysis.api;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;

public record SkinAnalysisRequest(
        @Positive Long captureId,
        @Min(0) @Max(120) Integer actualAge
) {
}
