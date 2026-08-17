package com.innerderma.carehistory.application;

import com.innerderma.caresolution.domain.CareSeason;
import com.innerderma.caresolution.domain.SafetyLevel;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record CareHistoryItem(
        LocalDate date,
        LocalDate eveningCareDate,
        LocalDate morningCareDate,
        Long captureId,
        Long analysisId,
        Long careCycleId,
        Long careSolutionId,
        CareProgressStatus progressStatus,
        CareSeason season,
        SafetyLevel safetyLevel,
        String headline,
        String primaryConcern,
        boolean hasSelfCheck,
        LocalDateTime generatedAt
) {}
