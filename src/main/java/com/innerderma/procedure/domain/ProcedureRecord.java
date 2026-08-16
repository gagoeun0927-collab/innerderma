package com.innerderma.procedure.domain;

import com.innerderma.facility.domain.Facility;
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
@Table(name = "procedure_records")
public class ProcedureRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "facility_id", nullable = false)
    private Facility facility;

    @Column(name = "procedure_date", nullable = false)
    private LocalDate procedureDate;

    @Column(name = "procedure_name", nullable = false, length = 100)
    private String procedureName;

    @Column(name = "care_guide", nullable = false, length = 1000)
    private String careGuide;

    protected ProcedureRecord() {
    }

    public ProcedureRecord(
            User user,
            Facility facility,
            LocalDate procedureDate,
            String procedureName,
            String careGuide
    ) {
        this.user = user;
        this.facility = facility;
        this.procedureDate = procedureDate;
        this.procedureName = procedureName;
        this.careGuide = careGuide;
    }

    public Long getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public Facility getFacility() {
        return facility;
    }

    public LocalDate getProcedureDate() {
        return procedureDate;
    }

    public String getProcedureName() {
        return procedureName;
    }

    public String getCareGuide() {
        return careGuide;
    }
}
