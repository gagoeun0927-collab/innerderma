package com.innerderma.procedure.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;

import java.time.LocalDate;
import java.util.List;

public interface ProcedureRecordRepository extends JpaRepository<ProcedureRecord, Long> {
    @EntityGraph(attributePaths = "facility")
    List<ProcedureRecord> findAllByUser_UserCodeAndFacility_FacilityCodeAndProcedureDate(
            String userCode,
            String facilityCode,
            LocalDate procedureDate
    );

    boolean existsByUser_UserCodeAndFacility_FacilityCodeAndProcedureDate(
            String userCode,
            String facilityCode,
            LocalDate procedureDate
    );
}
