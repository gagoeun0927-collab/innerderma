package com.innerderma.dailycare.application;

import java.time.LocalDate;
import java.util.List;

public record DailyCareResult(LocalDate servedDate, List<DailyCarePhaseResult> phases) {}
