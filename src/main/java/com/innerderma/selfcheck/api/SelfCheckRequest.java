package com.innerderma.selfcheck.api;

import com.innerderma.selfcheck.application.SelfCheckCommand;
import com.innerderma.selfcheck.domain.SymptomSeverity;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record SelfCheckRequest(
        @NotNull SymptomSeverity pain,
        @NotNull SymptomSeverity heatSensation,
        @NotNull SymptomSeverity tightness,
        @NotNull SymptomSeverity dryness,
        @NotNull SymptomSeverity itching,
        @NotNull SymptomSeverity swelling,
        @NotNull SymptomSeverity peeling,
        @NotNull SymptomSeverity breakout,
        @NotNull SymptomSeverity oozing,
        @NotNull SymptomSeverity bleeding,
        @NotNull SymptomSeverity barrierDamage,
        @Size(max = 500) String note
) {
    SelfCheckCommand toCommand() {
        return new SelfCheckCommand(
                pain, heatSensation, tightness, dryness, itching, swelling, peeling, breakout,
                oozing, bleeding, barrierDamage, note
        );
    }
}
