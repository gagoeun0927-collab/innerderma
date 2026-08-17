package com.innerderma.skincapture.api;

import com.innerderma.skincapture.domain.SkinCapture;
import com.innerderma.skincapture.domain.SkinCaptureQualityStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record SkinCaptureResponse(
        Long id,
        String userCode,
        LocalDate capturedDate,
        LocalDateTime capturedAt,
        String originalFilename,
        String contentType,
        long fileSize,
        SkinCaptureQualityStatus qualityStatus
) {
    public static SkinCaptureResponse from(SkinCapture capture) {
        return new SkinCaptureResponse(
                capture.getId(),
                capture.getUser().getUserCode(),
                capture.getCapturedDate(),
                capture.getCapturedAt(),
                capture.getOriginalFilename(),
                capture.getContentType(),
                capture.getFileSize(),
                capture.getQualityStatus()
        );
    }
}
