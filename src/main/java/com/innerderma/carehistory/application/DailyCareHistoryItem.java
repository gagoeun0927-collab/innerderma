package com.innerderma.carehistory.application;

public record DailyCareHistoryItem(CarePhase phase, boolean inherited,
                                   CareGenerationType generationType, CareHistoryItem history) {
    public DailyCareHistoryItem(CarePhase phase, boolean inherited, CareHistoryItem history) {
        this(phase, inherited, CareGenerationType.of(inherited), history);
    }
}
