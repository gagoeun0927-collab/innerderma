package com.innerderma.skindiagnosis.domain;

import com.innerderma.user.domain.User;
import jakarta.persistence.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Entity
@Table(name = "whs_skin_diagnoses")
public class WhsSkinDiagnosis {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "diagnosed_date", nullable = false)
    private LocalDate diagnosedDate;

    /** Kept only for consumers of the pre-metric API; metrics are the decision data. */
    @Column(name = "result_summary", nullable = false, length = 1000)
    private String resultSummary;

    @OneToMany(mappedBy = "diagnosis", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @OrderBy("metricType ASC")
    private List<WhsSkinDiagnosisMetric> metrics = new ArrayList<>();

    protected WhsSkinDiagnosis() { }

    /** @deprecated use the metric constructor for newly imported WHS diagnoses. */
    @Deprecated
    public WhsSkinDiagnosis(User user, LocalDate diagnosedDate, String resultSummary) {
        this(user, diagnosedDate, resultSummary, List.of());
    }

    public WhsSkinDiagnosis(User user, LocalDate diagnosedDate, String resultSummary,
                            Collection<WhsSkinDiagnosisMetric> metrics) {
        this.user = user;
        this.diagnosedDate = diagnosedDate;
        this.resultSummary = resultSummary;
        metrics.forEach(this::addMetric);
    }

    public void addMetric(WhsSkinDiagnosisMetric metric) {
        if (metrics.stream().anyMatch(existing -> existing.getMetricType() == metric.getMetricType())) {
            throw new IllegalArgumentException("A diagnosis can contain each metric type only once: " + metric.getMetricType());
        }
        metric.assignDiagnosis(this);
        metrics.add(metric);
    }
    public Long getId() { return id; }
    public User getUser() { return user; }
    public LocalDate getDiagnosedDate() { return diagnosedDate; }
    public String getResultSummary() { return resultSummary; }
    public List<WhsSkinDiagnosisMetric> getMetrics() { return List.copyOf(metrics); }
}
