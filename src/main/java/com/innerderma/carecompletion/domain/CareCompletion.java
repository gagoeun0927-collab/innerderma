package com.innerderma.carecompletion.domain;

import com.innerderma.carehistory.application.CarePhase;
import com.innerderma.caresolution.domain.CareSolution;
import com.innerderma.user.domain.User;
import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "care_completions", uniqueConstraints = @UniqueConstraint(
        name = "uk_care_completion_user_date_phase", columnNames = {"user_id", "served_date", "phase"}))
public class CareCompletion {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "care_solution_id", nullable = false)
    private CareSolution careSolution;

    @Column(name = "served_date", nullable = false)
    private LocalDate servedDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CarePhase phase;

    @Column(nullable = false)
    private boolean completed;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected CareCompletion() {}

    public CareCompletion(User user, CareSolution careSolution, LocalDate servedDate,
                          CarePhase phase, boolean completed, LocalDateTime updatedAt) {
        this.user = user;
        this.careSolution = careSolution;
        this.servedDate = servedDate;
        this.phase = phase;
        this.completed = completed;
        this.updatedAt = updatedAt;
    }

    public void update(CareSolution careSolution, boolean completed, LocalDateTime updatedAt) {
        this.careSolution = careSolution;
        this.completed = completed;
        this.updatedAt = updatedAt;
    }

    public Long getId() { return id; }
    public User getUser() { return user; }
    public CareSolution getCareSolution() { return careSolution; }
    public LocalDate getServedDate() { return servedDate; }
    public CarePhase getPhase() { return phase; }
    public boolean isCompleted() { return completed; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
