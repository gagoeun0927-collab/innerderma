package com.innerderma.skindiagnosis.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;

import java.util.Optional;
import java.time.LocalDate;

public interface WhsSkinDiagnosisRepository extends JpaRepository<WhsSkinDiagnosis, Long> {
    @EntityGraph(attributePaths = "user")
    Optional<WhsSkinDiagnosis> findTopByUser_UserCodeOrderByDiagnosedDateDesc(String userCode);

    @EntityGraph(attributePaths = "user")
    Optional<WhsSkinDiagnosis> findFirstByUser_UserCodeAndDiagnosedDateLessThanEqualOrderByDiagnosedDateDesc(
            String userCode, LocalDate date);

    boolean existsByUser_UserCode(String userCode);
}
