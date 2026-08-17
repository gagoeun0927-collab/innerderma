package com.innerderma.carehistory.application;

public record DailyCareHistoryItem(CarePhase phase, boolean inherited, CareHistoryItem history) {}
