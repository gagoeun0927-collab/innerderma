package com.innerderma.procedure.domain;

import com.innerderma.facility.domain.Facility;
import com.innerderma.user.domain.User;
import jakarta.persistence.Column;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

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

    @Column(name = "treatment_code", length = 100)
    private String treatmentCode;

    @Column(name = "treatment_type", length = 100)
    private String treatmentType;

    @Column(name = "treatment_area", length = 100)
    private String treatmentArea;

    @Column(name = "expected_recovery_days_min")
    private Integer expectedRecoveryDaysMin;

    @Column(name = "expected_recovery_days_max")
    private Integer expectedRecoveryDaysMax;

    @ElementCollection
    @CollectionTable(name = "procedure_normal_symptoms", joinColumns = @JoinColumn(name = "procedure_record_id"))
    @Column(name = "symptom", length = 500)
    private List<String> normalSymptoms = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "procedure_warning_symptoms", joinColumns = @JoinColumn(name = "procedure_record_id"))
    @Column(name = "symptom", length = 500)
    private List<String> warningSymptoms = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "procedure_aftercare_restrictions", joinColumns = @JoinColumn(name = "procedure_record_id"))
    @Column(name = "restriction_text", length = 500)
    private List<String> aftercareRestrictions = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "procedure_allowed_product_tags", joinColumns = @JoinColumn(name = "procedure_record_id"))
    @Column(name = "product_tag", length = 100)
    private List<String> allowedProductTags = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "procedure_restricted_product_tags", joinColumns = @JoinColumn(name = "procedure_record_id"))
    @Column(name = "product_tag", length = 100)
    private List<String> restrictedProductTags = new ArrayList<>();

    @Column(name = "treatment_source", length = 100)
    private String source;

    @Column(name = "treatment_rule_version", length = 50)
    private String ruleVersion;

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

    public ProcedureRecord(
            User user,
            Facility facility,
            LocalDate procedureDate,
            String procedureName,
            String careGuide,
            String treatmentCode,
            String treatmentType,
            String treatmentArea,
            Integer expectedRecoveryDaysMin,
            Integer expectedRecoveryDaysMax,
            List<String> normalSymptoms,
            List<String> warningSymptoms,
            List<String> aftercareRestrictions,
            List<String> allowedProductTags,
            List<String> restrictedProductTags,
            String source,
            String ruleVersion
    ) {
        this(user, facility, procedureDate, procedureName, careGuide);
        this.treatmentCode = treatmentCode;
        this.treatmentType = treatmentType;
        this.treatmentArea = treatmentArea;
        this.expectedRecoveryDaysMin = expectedRecoveryDaysMin;
        this.expectedRecoveryDaysMax = expectedRecoveryDaysMax;
        this.normalSymptoms = copyOf(normalSymptoms);
        this.warningSymptoms = copyOf(warningSymptoms);
        this.aftercareRestrictions = copyOf(aftercareRestrictions);
        this.allowedProductTags = copyOf(allowedProductTags);
        this.restrictedProductTags = copyOf(restrictedProductTags);
        this.source = source;
        this.ruleVersion = ruleVersion;
    }

    private static List<String> copyOf(List<String> values) {
        return values == null ? new ArrayList<>() : new ArrayList<>(values);
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

    public String getTreatmentCode() { return treatmentCode; }

    public String getTreatmentType() { return treatmentType; }

    public String getTreatmentArea() { return treatmentArea; }

    public Integer getExpectedRecoveryDaysMin() { return expectedRecoveryDaysMin; }

    public Integer getExpectedRecoveryDaysMax() { return expectedRecoveryDaysMax; }

    public List<String> getNormalSymptoms() { return List.copyOf(normalSymptoms); }

    public List<String> getWarningSymptoms() { return List.copyOf(warningSymptoms); }

    public List<String> getAftercareRestrictions() { return List.copyOf(aftercareRestrictions); }

    public List<String> getAllowedProductTags() { return List.copyOf(allowedProductTags); }

    public List<String> getRestrictedProductTags() { return List.copyOf(restrictedProductTags); }

    public String getSource() { return source; }

    public String getRuleVersion() { return ruleVersion; }
}
