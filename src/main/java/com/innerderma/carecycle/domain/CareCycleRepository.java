package com.innerderma.carecycle.domain;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;

public interface CareCycleRepository extends JpaRepository<CareCycle, Long> {
    boolean existsBySkinAnalysis_Id(Long skinAnalysisId);

    @EntityGraph(attributePaths = {"user", "skinAnalysis", "skinAnalysis.skinCapture", "selfCheck"})
    Optional<CareCycle> findByIdAndUser_UserCode(Long id, String userCode);

    @EntityGraph(attributePaths = {"user", "skinAnalysis", "skinAnalysis.skinCapture", "selfCheck"})
    Optional<CareCycle> findFirstByUser_UserCodeAndOriginCaptureDateLessThanEqualOrderByOriginCaptureDateDescCreatedAtDesc(
            String userCode, LocalDate targetDate);
}
