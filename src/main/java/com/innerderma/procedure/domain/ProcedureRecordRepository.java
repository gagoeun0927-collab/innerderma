package com.innerderma.procedure.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

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

    @EntityGraph(attributePaths = "facility")
    Optional<ProcedureRecord> findFirstByUser_UserCodeAndProcedureDateLessThanEqualOrderByProcedureDateDesc(
            String userCode, LocalDate date);

    @EntityGraph(attributePaths = "facility")
    Optional<ProcedureRecord> findFirstByUser_UserCodeAndProcedureDateLessThanEqualOrderByProcedureDateDescIdDesc(
            String userCode, LocalDate date);

    @EntityGraph(attributePaths = "facility")
    Optional<ProcedureRecord> findByIdAndUser_UserCode(Long id, String userCode);
}
