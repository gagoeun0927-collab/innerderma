package com.innerderma.selfcheck.application;

import com.innerderma.selfcheck.domain.SelfCheck;

import java.time.LocalDate;
import java.util.List;

public record SelfCheckHistoryResult(LocalDate from, LocalDate to, List<SelfCheck> items) {}
