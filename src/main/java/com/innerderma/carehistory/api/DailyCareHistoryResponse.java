package com.innerderma.carehistory.api;

import com.innerderma.carehistory.application.DailyCareHistoryItem;
import com.innerderma.carehistory.application.DailyCareHistoryResult;

import java.time.LocalDate;
import java.util.List;

public record DailyCareHistoryResponse(LocalDate servedDate, int count,
                                       List<DailyCareHistoryItem> items) {
    public static DailyCareHistoryResponse from(DailyCareHistoryResult result) {
        return new DailyCareHistoryResponse(result.servedDate(), result.items().size(), result.items());
    }
}
