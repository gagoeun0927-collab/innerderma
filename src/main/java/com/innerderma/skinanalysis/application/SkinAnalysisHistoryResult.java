package com.innerderma.skinanalysis.application;

import com.innerderma.skinanalysis.domain.SkinAnalysis;

import java.time.LocalDate;
import java.util.List;

public record SkinAnalysisHistoryResult(LocalDate from, LocalDate to, List<SkinAnalysis> items) {}
