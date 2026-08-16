package com.innerderma.skindiagnosis.domain;

import com.innerderma.user.domain.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.LocalDate;

@Entity
@Table(name = "whs_skin_diagnoses")
public class WhsSkinDiagnosis {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "diagnosed_date", nullable = false)
    private LocalDate diagnosedDate;

    @Column(name = "result_summary", nullable = false, length = 1000)
    private String resultSummary;

    protected WhsSkinDiagnosis() {
    }

    public WhsSkinDiagnosis(User user, LocalDate diagnosedDate, String resultSummary) {
        this.user = user;
        this.diagnosedDate = diagnosedDate;
        this.resultSummary = resultSummary;
    }

    public Long getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public LocalDate getDiagnosedDate() {
        return diagnosedDate;
    }

    public String getResultSummary() {
        return resultSummary;
    }
}
