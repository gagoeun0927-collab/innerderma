package com.innerderma.skindiagnosis.api;

import com.innerderma.skindiagnosis.domain.*;

public record WhsSkinDiagnosisMetricResponse(
        SkinDiagnosisMetricType metricType, Double userScore, Double averageScore, SkinDiagnosisGrade grade
) {
    static WhsSkinDiagnosisMetricResponse from(WhsSkinDiagnosisMetric metric) {
        return new WhsSkinDiagnosisMetricResponse(metric.getMetricType(), metric.getUserScore(),
                metric.getAverageScore(), metric.getGrade());
    }
}
