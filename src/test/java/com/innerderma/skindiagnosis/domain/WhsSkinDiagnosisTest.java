package com.innerderma.skindiagnosis.domain;

import com.innerderma.user.domain.User;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WhsSkinDiagnosisTest {
    @Test
    void preserves_null_scores_when_the_wellness_house_source_did_not_supply_them() {
        WhsSkinDiagnosis diagnosis = new WhsSkinDiagnosis(new User("WHS-1", "사용자", "010"),
                LocalDate.of(2026, 8, 15), "결과", List.of(
                new WhsSkinDiagnosisMetric(SkinDiagnosisMetricType.BLACKHEAD, null, null,
                        SkinDiagnosisGrade.NEEDS_IMPROVEMENT)));

        WhsSkinDiagnosisMetric metric = diagnosis.getMetrics().getFirst();
        assertThat(metric.getUserScore()).isNull();
        assertThat(metric.getAverageScore()).isNull();
        assertThat(metric.getGrade()).isEqualTo(SkinDiagnosisGrade.NEEDS_IMPROVEMENT);
    }

    @Test
    void rejects_duplicate_metric_types_in_one_diagnosis() {
        WhsSkinDiagnosis diagnosis = new WhsSkinDiagnosis(new User("WHS-1", "사용자", "010"),
                LocalDate.now(), "결과");
        diagnosis.addMetric(new WhsSkinDiagnosisMetric(SkinDiagnosisMetricType.PORE, 1.0, 2.0,
                SkinDiagnosisGrade.NORMAL));

        assertThatThrownBy(() -> diagnosis.addMetric(new WhsSkinDiagnosisMetric(SkinDiagnosisMetricType.PORE,
                3.0, 4.0, SkinDiagnosisGrade.EXCELLENT))).isInstanceOf(IllegalArgumentException.class);
    }
}
