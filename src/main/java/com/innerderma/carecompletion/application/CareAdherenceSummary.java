package com.innerderma.carecompletion.application;

import java.time.LocalDate;

public record CareAdherenceSummary(LocalDate from, LocalDate to,
                                   int recordedCount, int completedCount,
                                   int morningRecordedCount, int morningCompletedCount,
                                   int eveningRecordedCount, int eveningCompletedCount,
                                   int completionRatePercent) {}
