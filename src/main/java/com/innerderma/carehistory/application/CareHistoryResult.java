package com.innerderma.carehistory.application;

import java.time.LocalDate;
import java.util.List;

public record CareHistoryResult(LocalDate from, LocalDate to, List<CareHistoryItem> items) {}
