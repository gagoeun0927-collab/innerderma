package com.innerderma.selfcheck.application;

import com.innerderma.selfcheck.domain.SymptomSeverity;

public record SelfCheckCommand(
        SymptomSeverity pain,
        SymptomSeverity heatSensation,
        SymptomSeverity tightness,
        SymptomSeverity dryness,
        SymptomSeverity itching,
        SymptomSeverity swelling,
        SymptomSeverity peeling,
        SymptomSeverity breakout,
        SymptomSeverity oozing,
        SymptomSeverity bleeding,
        SymptomSeverity barrierDamage,
        String note
) {
}
