package com.innerderma.carecycle.domain;

import com.innerderma.selfcheck.domain.SelfCheck;
import com.innerderma.skinanalysis.domain.SkinAnalysis;
import com.innerderma.user.domain.User;
import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "care_cycles", uniqueConstraints =
        @UniqueConstraint(name = "uk_care_cycle_skin_analysis", columnNames = "skin_analysis_id"))
public class CareCycle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "skin_analysis_id", nullable = false)
    private SkinAnalysis skinAnalysis;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "self_check_id")
    private SelfCheck selfCheck;

    @Column(name = "origin_capture_date", nullable = false)
    private LocalDate originCaptureDate;

    @Column(name = "evening_care_date", nullable = false)
    private LocalDate eveningCareDate;

    @Column(name = "morning_care_date", nullable = false)
    private LocalDate morningCareDate;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    protected CareCycle() {}

    public CareCycle(User user, SkinAnalysis skinAnalysis, SelfCheck selfCheck,
                     LocalDate originCaptureDate, LocalDateTime createdAt) {
        this.user = user;
        this.skinAnalysis = skinAnalysis;
        this.selfCheck = selfCheck;
        this.originCaptureDate = originCaptureDate;
        this.eveningCareDate = originCaptureDate;
        this.morningCareDate = originCaptureDate.plusDays(1);
        this.createdAt = createdAt;
    }

    public Long getId() { return id; }
    public User getUser() { return user; }
    public SkinAnalysis getSkinAnalysis() { return skinAnalysis; }
    public SelfCheck getSelfCheck() { return selfCheck; }
    public LocalDate getOriginCaptureDate() { return originCaptureDate; }
    public LocalDate getEveningCareDate() { return eveningCareDate; }
    public LocalDate getMorningCareDate() { return morningCareDate; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
