package com.innerderma.selfcheck.domain;

import com.innerderma.user.domain.User;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class SelfCheckTest {

    private final User user = new User("WHS-DEMO-001", "테스트 사용자", "010-1234-1234");

    @Test
    void moderatePainRequiresSafetyAttention() {
        SelfCheck selfCheck = selfCheck(
                SymptomSeverity.MODERATE,
                SymptomSeverity.NONE,
                SymptomSeverity.NONE,
                SymptomSeverity.MILD
        );

        assertThat(selfCheck.requiresSafetyAttention()).isTrue();
    }

    @Test
    void severeNonProcedureSymptomAlsoRequiresSafetyAttention() {
        SelfCheck selfCheck = selfCheck(
                SymptomSeverity.NONE,
                SymptomSeverity.NONE,
                SymptomSeverity.NONE,
                SymptomSeverity.SEVERE
        );

        assertThat(selfCheck.requiresSafetyAttention()).isTrue();
    }

    @Test
    void mildSymptomsDoNotTriggerSafetyAttention() {
        SelfCheck selfCheck = selfCheck(
                SymptomSeverity.MILD,
                SymptomSeverity.MILD,
                SymptomSeverity.MILD,
                SymptomSeverity.MILD
        );

        assertThat(selfCheck.requiresSafetyAttention()).isFalse();
    }

    private SelfCheck selfCheck(
            SymptomSeverity pain,
            SymptomSeverity heat,
            SymptomSeverity swelling,
            SymptomSeverity itching
    ) {
        return new SelfCheck(
                user,
                LocalDateTime.of(2026, 8, 17, 12, 30),
                pain,
                heat,
                SymptomSeverity.NONE,
                SymptomSeverity.MILD,
                itching,
                swelling,
                SymptomSeverity.NONE,
                SymptomSeverity.NONE,
                "  오늘 상태  "
        );
    }
}
