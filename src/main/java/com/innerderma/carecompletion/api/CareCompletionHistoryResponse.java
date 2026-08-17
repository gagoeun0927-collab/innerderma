package com.innerderma.carecompletion.api;

import com.innerderma.carecompletion.application.CareCompletionHistoryResult;
import com.innerderma.carehistory.application.CarePhase;

import java.time.LocalDate;
import java.util.*;

public record CareCompletionHistoryResponse(LocalDate from, LocalDate to, List<Day> days) {
    public static CareCompletionHistoryResponse from(CareCompletionHistoryResult result) {
        Map<LocalDate, EnumMap<CarePhase, Boolean>> grouped = new LinkedHashMap<>();
        result.items().forEach(item -> grouped
                .computeIfAbsent(item.getServedDate(), ignored -> new EnumMap<>(CarePhase.class))
                .put(item.getPhase(), item.isCompleted()));
        List<Day> days = grouped.entrySet().stream().map(entry -> new Day(entry.getKey(),
                entry.getValue().containsKey(CarePhase.MORNING),
                entry.getValue().getOrDefault(CarePhase.MORNING, false),
                entry.getValue().containsKey(CarePhase.EVENING),
                entry.getValue().getOrDefault(CarePhase.EVENING, false))).toList();
        return new CareCompletionHistoryResponse(result.from(), result.to(), days);
    }

    public record Day(LocalDate date, boolean morningRecorded, boolean morningCompleted,
                      boolean eveningRecorded, boolean eveningCompleted) {}
}
