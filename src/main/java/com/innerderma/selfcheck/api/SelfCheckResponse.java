package com.innerderma.selfcheck.api;

import com.innerderma.selfcheck.domain.SelfCheck;
import com.innerderma.selfcheck.domain.SymptomSeverity;

import java.time.LocalDateTime;

public record SelfCheckResponse(
        Long id,
        String userCode,
        LocalDateTime checkedAt,
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
        String note,
        boolean requiresSafetyAttention
) {
    public static SelfCheckResponse from(SelfCheck selfCheck) {
        return new SelfCheckResponse(
                selfCheck.getId(),
                selfCheck.getUser().getUserCode(),
                selfCheck.getCheckedAt(),
                selfCheck.getPain(),
                selfCheck.getHeatSensation(),
                selfCheck.getTightness(),
                selfCheck.getDryness(),
                selfCheck.getItching(),
                selfCheck.getSwelling(),
                selfCheck.getPeeling(),
                selfCheck.getBreakout(),
                selfCheck.getOozing(),
                selfCheck.getBleeding(),
                selfCheck.getBarrierDamage(),
                selfCheck.getNote(),
                selfCheck.requiresSafetyAttention()
        );
    }
}
