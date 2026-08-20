package com.innerderma.skinstate.domain;

import com.innerderma.user.domain.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 사용자의 하루 피부 상태 스냅샷 (MVP: 자가문진 원천 기반).
 *
 * <p>자가문진 8개 항목을 원천 축으로 그대로 보존하고, 점수는 {@code scoringVersion} 규약에 따라 산정된
 * 결정적 기술 점수의 JSON 맵으로 저장한다. 임상 중증도 해석이 아니며, R003 taxonomy 로의 변환은
 * 별도 정책 확정 후 상위 계층에서 수행한다. SkinAge 분석은 필수 입력이 아니라 선택적 참조로만 둔다.
 * {@code (user, snapshotDate)} 를 유니크로 두어 하루 1개를 유지하고 재생성 시 갱신한다.
 */
@Entity
@Table(name = "skin_state_snapshots", uniqueConstraints =
        @UniqueConstraint(name = "uk_snapshot_user_date", columnNames = {"user_id", "snapshot_date"}))
public class SkinStateSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "snapshot_date", nullable = false)
    private LocalDate snapshotDate;

    @Column(name = "scoring_version", nullable = false, length = 40)
    private String scoringVersion;

    @Lob
    @Column(name = "symptom_scores_json", nullable = false, length = Integer.MAX_VALUE)
    private String symptomScoresJson;

    @Lob
    @Column(name = "analysis_scores_json", length = Integer.MAX_VALUE)
    private String analysisScoresJson;

    @Column(name = "dominant_symptom", length = 40)
    private String dominantSymptom;

    @Column(name = "source_self_check_id", nullable = false)
    private Long sourceSelfCheckId;

    @Column(name = "source_analysis_id")
    private Long sourceAnalysisId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected SkinStateSnapshot() {
    }

    public SkinStateSnapshot(User user, LocalDate snapshotDate, String scoringVersion,
                             String symptomScoresJson, String analysisScoresJson, String dominantSymptom,
                             Long sourceSelfCheckId, Long sourceAnalysisId, LocalDateTime now) {
        this.user = user;
        this.snapshotDate = snapshotDate;
        this.scoringVersion = scoringVersion;
        this.symptomScoresJson = symptomScoresJson;
        this.analysisScoresJson = analysisScoresJson;
        this.dominantSymptom = dominantSymptom;
        this.sourceSelfCheckId = sourceSelfCheckId;
        this.sourceAnalysisId = sourceAnalysisId;
        this.createdAt = now;
        this.updatedAt = now;
    }

    /** 같은 현지 날짜에 재생성될 때 기존 스냅샷을 결정적으로 갱신한다. */
    public void applyScoring(String scoringVersion, String symptomScoresJson, String analysisScoresJson,
                             String dominantSymptom, Long sourceSelfCheckId, Long sourceAnalysisId,
                             LocalDateTime now) {
        this.scoringVersion = scoringVersion;
        this.symptomScoresJson = symptomScoresJson;
        this.analysisScoresJson = analysisScoresJson;
        this.dominantSymptom = dominantSymptom;
        this.sourceSelfCheckId = sourceSelfCheckId;
        this.sourceAnalysisId = sourceAnalysisId;
        this.updatedAt = now;
    }

    /** SkinAge 분석 완료 시 analysisScoresJson만 업데이트한다. */
    public void applyAnalysisScores(Long sourceAnalysisId, String analysisScoresJson, LocalDateTime now) {
        this.sourceAnalysisId = sourceAnalysisId;
        this.analysisScoresJson = analysisScoresJson;
        this.updatedAt = now;
    }

    public Long getId() { return id; }
    public User getUser() { return user; }
    public LocalDate getSnapshotDate() { return snapshotDate; }
    public String getScoringVersion() { return scoringVersion; }
    public String getSymptomScoresJson() { return symptomScoresJson; }
    public String getAnalysisScoresJson() { return analysisScoresJson; }
    public String getDominantSymptom() { return dominantSymptom; }
    public Long getSourceSelfCheckId() { return sourceSelfCheckId; }
    public Long getSourceAnalysisId() { return sourceAnalysisId; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
