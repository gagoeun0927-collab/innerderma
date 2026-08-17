package com.innerderma.skinanalysis.domain;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SkinAnalysisRepository extends JpaRepository<SkinAnalysis, Long> {
    boolean existsBySkinCapture_Id(Long skinCaptureId);

    @EntityGraph(attributePaths = {"skinCapture", "skinCapture.user"})
    Optional<SkinAnalysis> findFirstBySkinCapture_User_UserCodeOrderByAnalyzedAtDesc(String userCode);
}
