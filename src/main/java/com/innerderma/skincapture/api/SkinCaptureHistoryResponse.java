package com.innerderma.skincapture.api;

import com.innerderma.skincapture.application.SkinCaptureHistoryResult;

import java.time.LocalDate;
import java.util.List;

public record SkinCaptureHistoryResponse(LocalDate from, LocalDate to, int count,
                                         List<SkinCaptureResponse> items) {
    public static SkinCaptureHistoryResponse from(SkinCaptureHistoryResult result) {
        List<SkinCaptureResponse> items = result.items().stream()
                .map(SkinCaptureResponse::from)
                .toList();
        return new SkinCaptureHistoryResponse(result.from(), result.to(), items.size(), items);
    }
}
