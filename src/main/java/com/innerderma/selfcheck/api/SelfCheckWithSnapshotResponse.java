package com.innerderma.selfcheck.api;

import com.innerderma.selfcheck.domain.SelfCheck;
import com.innerderma.skinstate.application.SkinStateSnapshotResult;

import java.time.LocalDate;
import java.util.Map;

public record SelfCheckWithSnapshotResponse(
        SelfCheckResponse selfCheck,
        SnapshotSummary snapshot
) {
    public static SelfCheckWithSnapshotResponse from(SelfCheck selfCheck, SkinStateSnapshotResult snapshotResult) {
        return new SelfCheckWithSnapshotResponse(
                SelfCheckResponse.from(selfCheck),
                new SnapshotSummary(
                        snapshotResult.snapshot().getId(),
                        snapshotResult.snapshot().getSnapshotDate(),
                        snapshotResult.snapshot().getDominantSymptom(),
                        snapshotResult.snapshot().getScoringVersion(),
                        snapshotResult.symptomScores()
                )
        );
    }

    public record SnapshotSummary(
            Long id,
            LocalDate snapshotDate,
            String dominantSymptom,
            String scoringVersion,
            Map<String, Integer> scores
    ) {}
}
