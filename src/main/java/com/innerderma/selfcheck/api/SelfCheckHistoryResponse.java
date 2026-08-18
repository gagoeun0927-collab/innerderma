package com.innerderma.selfcheck.api;

import com.innerderma.selfcheck.application.SelfCheckHistoryResult;

import java.time.LocalDate;
import java.util.List;

public record SelfCheckHistoryResponse(LocalDate from, LocalDate to, int count,
                                       List<SelfCheckResponse> items) {
    public static SelfCheckHistoryResponse from(SelfCheckHistoryResult result) {
        List<SelfCheckResponse> items = result.items().stream().map(SelfCheckResponse::from).toList();
        return new SelfCheckHistoryResponse(result.from(), result.to(), items.size(), items);
    }
}
