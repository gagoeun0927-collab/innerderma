package com.innerderma.skinanalysis.domain;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface SkinAnalysisRepository extends JpaRepository<SkinAnalysis, Long> {
    boolean existsBySkinCapture_Id(Long skinCaptureId);

    @EntityGraph(attributePaths = {"skinCapture", "skinCapture.user"})
    Optional<SkinAnalysis> findFirstBySkinCapture_User_UserCodeOrderByAnalyzedAtDesc(String userCode);

    @EntityGraph(attributePaths = {"skinCapture", "skinCapture.user"})
    Optional<SkinAnalysis> findBySkinCapture_Id(Long skinCaptureId);

    @EntityGraph(attributePaths = {"skinCapture", "skinCapture.user"})
    List<SkinAnalysis> findBySkinCapture_User_UserCodeAndAnalyzedAtBetweenOrderByAnalyzedAtDesc(
            String userCode, LocalDateTime from, LocalDateTime to);
}
