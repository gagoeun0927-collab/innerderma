package com.innerderma.carehistory.application;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CareGenerationTypeTest {

    @Test
    void inheritedSolutionIsCarriedForward() {
        assertThat(CareGenerationType.of(true)).isEqualTo(CareGenerationType.CARRIED_FORWARD);
    }

    @Test
    void freshSolutionIsNewAnalysis() {
        assertThat(CareGenerationType.of(false)).isEqualTo(CareGenerationType.NEW_ANALYSIS);
    }
}
