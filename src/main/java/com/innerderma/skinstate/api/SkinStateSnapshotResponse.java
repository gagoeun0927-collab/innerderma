package com.innerderma.skinstate.api;

import com.innerderma.skinstate.application.SkinStateSnapshotResult;
import com.innerderma.skinstate.domain.SkinStateSnapshot;

import java.time.LocalDate;
import java.util.Map;

public record SkinStateSnapshotResponse(
        Long id,
        String userCode,
        LocalDate snapshotDate,
        String scoringVersion,
        Map<String, Integer> symptomScores,
        String dominantSymptom,
        Long sourceSelfCheckId,
        Long sourceAnalysisId
) {
    public static SkinStateSnapshotResponse from(SkinStateSnapshotResult result) {
        SkinStateSnapshot snapshot = result.snapshot();
        return new SkinStateSnapshotResponse(
                snapshot.getId(),
                snapshot.getUser().getUserCode(),
                snapshot.getSnapshotDate(),
                snapshot.getScoringVersion(),
                result.symptomScores(),
                snapshot.getDominantSymptom(),
                snapshot.getSourceSelfCheckId(),
                snapshot.getSourceAnalysisId()
        );
    }
}
