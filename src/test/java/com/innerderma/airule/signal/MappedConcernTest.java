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
}
