package com.innerderma.skinanalysis.application;

import com.innerderma.skinanalysis.domain.SkinAnalysis;

public record SkinAnalysisResult(SkinAnalysis analysis, SkinAgeAnalysisResult result) {
}
