package com.innerderma.skindiagnosis.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;

import java.util.Optional;

public interface WhsSkinDiagnosisRepository extends JpaRepository<WhsSkinDiagnosis, Long> {
    @EntityGraph(attributePaths = "user")
    Optional<WhsSkinDiagnosis> findTopByUser_UserCodeOrderByDiagnosedDateDesc(String userCode);

    boolean existsByUser_UserCode(String userCode);
}
