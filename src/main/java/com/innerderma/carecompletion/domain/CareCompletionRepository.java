package com.innerderma.carecompletion.domain;

import com.innerderma.carehistory.application.CarePhase;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface CareCompletionRepository extends JpaRepository<CareCompletion, Long> {
    @EntityGraph(attributePaths = {"user", "careSolution", "careSolution.careCycle"})
    Optional<CareCompletion> findByUser_UserCodeAndServedDateAndPhase(
            String userCode, LocalDate servedDate, CarePhase phase);

    @EntityGraph(attributePaths = {"user", "careSolution", "careSolution.careCycle"})
    List<CareCompletion> findByUser_UserCodeAndServedDateOrderByPhaseAsc(
            String userCode, LocalDate servedDate);
}
