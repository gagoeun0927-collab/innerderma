package com.innerderma.skinanalysis.domain;

import com.innerderma.skincapture.domain.SkinCapture;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "skin_analyses")
public class SkinAnalysis {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "skin_capture_id", nullable = false, unique = true)
    private SkinCapture skinCapture;

    @Column(name = "analyzed_at", nullable = false)
    private LocalDateTime analyzedAt;

    @Column(name = "overall_score", nullable = false)
    private double overallScore;

    @Column(name = "skin_health_grade", nullable = false, length = 50)
    private String skinHealthGrade;

    @Column(name = "model_version", nullable = false, length = 50)
    private String modelVersion;

    @Lob
    @Column(name = "raw_result", nullable = false)
    private String rawResult;

    protected SkinAnalysis() {}

    public SkinAnalysis(
            SkinCapture skinCapture,
            LocalDateTime analyzedAt,
            double overallScore,
            String skinHealthGrade,
            String modelVersion,
            String rawResult
    ) {
        this.skinCapture = skinCapture;
        this.analyzedAt = analyzedAt;
        this.overallScore = overallScore;
        this.skinHealthGrade = skinHealthGrade;
        this.modelVersion = modelVersion;
        this.rawResult = rawResult;
    }

    public Long getId() { return id; }
    public SkinCapture getSkinCapture() { return skinCapture; }
    public LocalDateTime getAnalyzedAt() { return analyzedAt; }
    public double getOverallScore() { return overallScore; }
    public String getSkinHealthGrade() { return skinHealthGrade; }
    public String getModelVersion() { return modelVersion; }
    public String getRawResult() { return rawResult; }
}
