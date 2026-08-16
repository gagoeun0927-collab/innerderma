package com.innerderma.facility.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "facilities")
public class Facility {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "facility_code", nullable = false, unique = true, length = 20)
    private String facilityCode;

    @Column(nullable = false, length = 100)
    private String name;

    protected Facility() {
    }

    public Facility(String facilityCode, String name) {
        this.facilityCode = facilityCode;
        this.name = name;
    }

    public Long getId() {
        return id;
    }

    public String getFacilityCode() {
        return facilityCode;
    }

    public String getName() {
        return name;
    }
}
