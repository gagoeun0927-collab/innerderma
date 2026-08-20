package com.innerderma.caresolution.domain;

import com.innerderma.carecycle.domain.CareCycle;
import com.innerderma.procedure.domain.ProcedureRecord;
import com.innerderma.skindiagnosis.domain.WhsSkinDiagnosis;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "care_solutions", uniqueConstraints =
        @UniqueConstraint(name = "uk_care_solution_cycle", columnNames = "care_cycle_id"))
public class CareSolution {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "care_cycle_id", nullable = false)
    private CareCycle careCycle;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "whs_diagnosis_id")
    private WhsSkinDiagnosis whsDiagnosis;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "procedure_record_id")
    private ProcedureRecord procedureRecord;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CareSeason season;

    @Enumerated(EnumType.STRING)
    @Column(name = "safety_level", nullable = false, length = 20)
    private SafetyLevel safetyLevel;

    @Column(nullable = false, length = 200)
    private String headline;

    @Lob @Column(name = "evening_steps_json", nullable = false, length = Integer.MAX_VALUE)
    private String eveningStepsJson;

    @Lob @Column(name = "morning_steps_json", nullable = false, length = Integer.MAX_VALUE)
    private String morningStepsJson;

    @Column(name = "safety_message", length = 1000)
    private String safetyMessage;

    @Column(name = "primary_concern", length = 30)
    private String primaryConcern;

    @Column(name = "generated_at", nullable = false)
    private LocalDateTime generatedAt;

    protected CareSolution() {}

    public CareSolution(CareCycle careCycle, WhsSkinDiagnosis whsDiagnosis,
                        ProcedureRecord procedureRecord, CareSeason season,
                        SafetyLevel safetyLevel, String headline, String eveningStepsJson,
                        String morningStepsJson, String safetyMessage, String primaryConcern,
                        LocalDateTime generatedAt) {
        this.careCycle = careCycle;
        this.whsDiagnosis = whsDiagnosis;
        this.procedureRecord = procedureRecord;
        this.season = season;
        this.safetyLevel = safetyLevel;
        this.headline = headline;
        this.eveningStepsJson = eveningStepsJson;
        this.morningStepsJson = morningStepsJson;
        this.safetyMessage = safetyMessage;
        this.primaryConcern = primaryConcern;
        this.generatedAt = generatedAt;
    }

    public Long getId() { return id; }
    public CareCycle getCareCycle() { return careCycle; }
    public WhsSkinDiagnosis getWhsDiagnosis() { return whsDiagnosis; }
    public ProcedureRecord getProcedureRecord() { return procedureRecord; }
    public CareSeason getSeason() { return season; }
    public SafetyLevel getSafetyLevel() { return safetyLevel; }
    public String getHeadline() { return headline; }
    public String getEveningStepsJson() { return eveningStepsJson; }
    public String getMorningStepsJson() { return morningStepsJson; }
    public String getSafetyMessage() { return safetyMessage; }
    public String getPrimaryConcern() { return primaryConcern; }
    public LocalDateTime getGeneratedAt() { return generatedAt; }
}
