package com.innerderma.skindiagnosis.application;

import com.innerderma.skindiagnosis.domain.WhsSkinDiagnosis;

import java.time.LocalDate;
import java.util.List;

public record WhsSkinDiagnosisHistoryResult(LocalDate from, LocalDate to, List<WhsSkinDiagnosis> items) {}
