package com.innerderma.carehistory.application;

import java.time.LocalDate;
import java.util.List;

public record DailyCareHistoryResult(LocalDate servedDate, List<DailyCareHistoryItem> items) {}
