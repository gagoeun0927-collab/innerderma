package com.innerderma.carehistory.api;

import com.innerderma.carehistory.application.CareHistoryItem;
import com.innerderma.carehistory.application.CareHistoryResult;

import java.time.LocalDate;
import java.util.List;

public record CareHistoryResponse(LocalDate from, LocalDate to, int count, List<CareHistoryItem> items) {
    public static CareHistoryResponse from(CareHistoryResult result) {
        return new CareHistoryResponse(result.from(), result.to(), result.items().size(), result.items());
    }
}
