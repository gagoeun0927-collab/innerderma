package com.innerderma.selfcheck.domain;

import com.innerderma.user.domain.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.util.stream.Stream;

@Entity
@Table(name = "self_checks")
public class SelfCheck {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "checked_at", nullable = false)
    private LocalDateTime checkedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SymptomSeverity pain;

    @Enumerated(EnumType.STRING)
    @Column(name = "heat_sensation", nullable = false, length = 20)
    private SymptomSeverity heatSensation;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SymptomSeverity tightness;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SymptomSeverity dryness;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SymptomSeverity itching;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SymptomSeverity swelling;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SymptomSeverity peeling;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SymptomSeverity breakout;

    @Column(length = 500)
    private String note;

    protected SelfCheck() {
    }

    public SelfCheck(
            User user,
            LocalDateTime checkedAt,
            SymptomSeverity pain,
            SymptomSeverity heatSensation,
            SymptomSeverity tightness,
            SymptomSeverity dryness,
            SymptomSeverity itching,
            SymptomSeverity swelling,
            SymptomSeverity peeling,
            SymptomSeverity breakout,
            String note
    ) {
        this.user = user;
        this.checkedAt = checkedAt;
        this.pain = pain;
        this.heatSensation = heatSensation;
        this.tightness = tightness;
        this.dryness = dryness;
        this.itching = itching;
        this.swelling = swelling;
        this.peeling = peeling;
        this.breakout = breakout;
        this.note = normalizeNote(note);
    }

    public boolean requiresSafetyAttention() {
        boolean anySevere = Stream.of(
                pain, heatSensation, tightness, dryness, itching, swelling, peeling, breakout
        ).anyMatch(severity -> severity == SymptomSeverity.SEVERE);

        boolean procedureRiskSignal = Stream.of(pain, heatSensation, swelling)
                .anyMatch(severity -> severity == SymptomSeverity.MODERATE);
        return anySevere || procedureRiskSignal;
    }

    private String normalizeNote(String note) {
        return note == null || note.isBlank() ? null : note.trim();
    }

    public Long getId() { return id; }
    public User getUser() { return user; }
    public LocalDateTime getCheckedAt() { return checkedAt; }
    public SymptomSeverity getPain() { return pain; }
    public SymptomSeverity getHeatSensation() { return heatSensation; }
    public SymptomSeverity getTightness() { return tightness; }
    public SymptomSeverity getDryness() { return dryness; }
    public SymptomSeverity getItching() { return itching; }
    public SymptomSeverity getSwelling() { return swelling; }
    public SymptomSeverity getPeeling() { return peeling; }
    public SymptomSeverity getBreakout() { return breakout; }
    public String getNote() { return note; }
}
