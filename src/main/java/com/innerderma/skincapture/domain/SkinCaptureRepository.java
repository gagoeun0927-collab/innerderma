package com.innerderma.skincapture.domain;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface SkinCaptureRepository extends JpaRepository<SkinCapture, Long> {

    boolean existsByUser_UserCodeAndCapturedDateAndQualityStatus(
            String userCode,
            LocalDate capturedDate,
            SkinCaptureQualityStatus qualityStatus
    );

    @EntityGraph(attributePaths = "user")
    Optional<SkinCapture> findFirstByUser_UserCodeOrderByCapturedAtDesc(String userCode);

    @EntityGraph(attributePaths = "user")
    Optional<SkinCapture> findByIdAndUser_UserCode(Long id, String userCode);

    @EntityGraph(attributePaths = "user")
    Optional<SkinCapture> findFirstByUser_UserCodeAndCapturedDateAndQualityStatusOrderByCapturedAtDesc(
            String userCode, LocalDate capturedDate, SkinCaptureQualityStatus qualityStatus);

    @EntityGraph(attributePaths = "user")
    List<SkinCapture> findByUser_UserCodeAndCapturedDateBetweenAndQualityStatusOrderByCapturedDateDescCapturedAtDesc(
            String userCode, LocalDate from, LocalDate to, SkinCaptureQualityStatus qualityStatus);
}
