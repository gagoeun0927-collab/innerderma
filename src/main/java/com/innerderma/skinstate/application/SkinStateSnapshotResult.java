package com.innerderma.skinstate.application;

import com.innerderma.skinstate.domain.SkinStateSnapshot;

import java.util.Map;

/** Snapshot 엔티티와 파싱된 증상 점수 맵을 함께 전달한다. */
public record SkinStateSnapshotResult(SkinStateSnapshot snapshot, Map<String, Integer> symptomScores) {
}
