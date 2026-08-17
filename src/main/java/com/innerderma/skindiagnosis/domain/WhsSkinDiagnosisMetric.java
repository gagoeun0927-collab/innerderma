package com.innerderma.skindiagnosis.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "whs_skin_diagnosis_metrics",
        uniqueConstraints = @UniqueConstraint(name = "uk_whs_diagnosis_metric", columnNames = {"diagnosis_id", "metric_type"}))
public class WhsSkinDiagnosisMetric {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "diagnosis_id", nullable = false)
    private WhsSkinDiagnosis diagnosis;

    @Enumerated(EnumType.STRING)
    @Column(name = "metric_type", nullable = false, length = 40)
    private SkinDiagnosisMetricType metricType;

    /** Raw device score. It remains null when WHS did not provide a score. */
    @Column(name = "user_score")
    private Double userScore;

    /** Raw comparison-group score. Never calculated or inferred by this service. */
    @Column(name = "average_score")
    private Double averageScore;

    @Enumerated(EnumType.STRING)
    @Column(name = "grade", length = 30)
    private SkinDiagnosisGrade grade;

    protected WhsSkinDiagnosisMetric() { }

    public WhsSkinDiagnosisMetric(SkinDiagnosisMetricType metricType, Double userScore,
                                  Double averageScore, SkinDiagnosisGrade grade) {
        this.metricType = metricType;
        this.userScore = userScore;
        this.averageScore = averageScore;
        this.grade = grade;
    }

    void assignDiagnosis(WhsSkinDiagnosis diagnosis) { this.diagnosis = diagnosis; }
    public Long getId() { return id; }
    public SkinDiagnosisMetricType getMetricType() { return metricType; }
    public Double getUserScore() { return userScore; }
    public Double getAverageScore() { return averageScore; }
    public SkinDiagnosisGrade getGrade() { return grade; }
}
