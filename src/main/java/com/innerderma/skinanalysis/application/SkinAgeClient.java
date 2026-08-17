package com.innerderma.skinanalysis.application;

public interface SkinAgeClient {
    SkinAgeAnalysisResult analyze(
            byte[] imageBytes,
            String filename,
            String contentType,
            Integer actualAge
    );
}
