package com.innerderma.airule.signal;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MappedConcernTest {

    @Test
    void mapsDrynessToHydration() {
        MappedConcern result = MappedConcern.fromSelfReport("dryness");
        assertThat(result).isNotNull();
        assertThat(result.concern()).isEqualTo("HYDRATION");
        assertThat(result.confidence()).isEqualTo(0.72);
        assertThat(result.sources()).containsExactly("SELF_REPORT");
    }

    @Test
    void mapsBreakoutToAcne() {
        MappedConcern result = MappedConcern.fromSelfReport("breakout");
        assertThat(result.concern()).isEqualTo("ACNE");
    }

    @Test
    void mapsPeelingToBarrierRecovery() {
        MappedConcern result = MappedConcern.fromSelfReport("peeling");
        assertThat(result.concern()).isEqualTo("BARRIER_RECOVERY");
    }

    @Test
    void mapsHeatSensationToIrritation() {
        MappedConcern result = MappedConcern.fromSelfReport("heatSensation");
        assertThat(result.concern()).isEqualTo("IRRITATION");
    }

    @Test
    void returnsNullForNullOrBlankInput() {
        assertThat(MappedConcern.fromSelfReport(null)).isNull();
        assertThat(MappedConcern.fromSelfReport("")).isNull();
    }

    @Test
    void mapsImageAnalysisWorstConcernToTaxonomy() {
        MappedConcern result = MappedConcern.fromImageAnalysis(
                java.util.Map.of("wrinkle", 50.0, "redness", 30.0, "pigmentation", 60.0, "pore_texture", 45.0));
        assertThat(result).isNotNull();
        assertThat(result.concern()).isEqualTo("REDNESS"); // redness 30.0이 가장 낮음(가장 심함)
        assertThat(result.confidence()).isEqualTo(0.85);
        assertThat(result.sources()).containsExactly("IMAGE_ANALYSIS");
    }

    @Test
    void skipsWrinkleInImageAnalysis() {
        MappedConcern result = MappedConcern.fromImageAnalysis(
                java.util.Map.of("wrinkle", 10.0, "redness", 80.0, "pigmentation", 90.0, "pore_texture", 85.0));
        // wrinkle은 스킵하므로 redness(80)가 가장 낮음
        assertThat(result.concern()).isEqualTo("REDNESS");
    }

    @Test
    void combinesSameConcearnWithHigherConfidence() {
        MappedConcern self = MappedConcern.fromSelfReport("itching"); // → REDNESS
        MappedConcern image = MappedConcern.fromImageAnalysis(
                java.util.Map.of("redness", 20.0, "pigmentation", 80.0, "pore_texture", 90.0));
        MappedConcern combined = MappedConcern.combine(self, image);
        assertThat(combined.concern()).isEqualTo("REDNESS");
        assertThat(combined.confidence()).isEqualTo(0.92);
        assertThat(combined.sources()).containsExactly("SELF_REPORT", "IMAGE_ANALYSIS");
    }

    @Test
    void combinesDifferentConcernsPreferringImageAnalysis() {
        MappedConcern self = MappedConcern.fromSelfReport("dryness"); // → HYDRATION
        MappedConcern image = MappedConcern.fromImageAnalysis(
                java.util.Map.of("redness", 20.0, "pigmentation", 80.0, "pore_texture", 90.0));
        MappedConcern combined = MappedConcern.combine(self, image);
        assertThat(combined.concern()).isEqualTo("REDNESS"); // image 우선
    }
}
