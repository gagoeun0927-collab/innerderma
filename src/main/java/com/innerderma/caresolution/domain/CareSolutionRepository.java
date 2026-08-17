package com.innerderma.caresolution.domain;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface CareSolutionRepository extends JpaRepository<CareSolution, Long> {
    boolean existsByCareCycle_Id(Long careCycleId);

    @EntityGraph(attributePaths = {"careCycle", "careCycle.user", "careCycle.skinAnalysis",
            "careCycle.skinAnalysis.skinCapture", "careCycle.selfCheck", "whsDiagnosis",
            "procedureRecord", "procedureRecord.facility"})
    Optional<CareSolution> findFirstByCareCycle_User_UserCodeAndCareCycle_OriginCaptureDateLessThanEqualOrderByCareCycle_OriginCaptureDateDescGeneratedAtDesc(
            String userCode, LocalDate date);

    @EntityGraph(attributePaths = {"careCycle", "careCycle.user", "careCycle.skinAnalysis",
            "careCycle.skinAnalysis.skinCapture", "careCycle.selfCheck"})
    List<CareSolution> findByCareCycle_User_UserCodeAndCareCycle_OriginCaptureDateBetweenOrderByCareCycle_OriginCaptureDateDesc(
            String userCode, LocalDate from, LocalDate to);

    @EntityGraph(attributePaths = {"careCycle", "careCycle.user", "careCycle.skinAnalysis",
            "careCycle.skinAnalysis.skinCapture", "careCycle.selfCheck"})
    Optional<CareSolution> findByCareCycle_Id(Long careCycleId);
}
