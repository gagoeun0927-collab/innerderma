package com.innerderma.skincapture.api;

import com.innerderma.skincapture.application.DailyCaptureStatus;
import java.time.LocalDate;

public record DailyCaptureStatusResponse(LocalDate date, boolean canCapture, SkinCaptureResponse capture) {
    public static DailyCaptureStatusResponse from(DailyCaptureStatus status) {
        return new DailyCaptureStatusResponse(status.date(), status.canCapture(),
                status.capture() == null ? null : SkinCaptureResponse.from(status.capture()));
    }
}
