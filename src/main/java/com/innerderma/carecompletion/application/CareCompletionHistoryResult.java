package com.innerderma.carecompletion.application;

import com.innerderma.carecompletion.domain.CareCompletion;

import java.time.LocalDate;
import java.util.List;

public record CareCompletionHistoryResult(LocalDate from, LocalDate to,
                                          List<CareCompletion> items) {}
