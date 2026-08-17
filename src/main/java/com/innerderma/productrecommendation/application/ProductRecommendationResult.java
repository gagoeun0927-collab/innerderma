package com.innerderma.productrecommendation.application;

import com.innerderma.caresolution.domain.SafetyLevel;

import java.time.LocalDate;
import java.util.List;

public record ProductRecommendationResult(LocalDate originCaptureDate, LocalDate servedDate,
                                          boolean inherited, SafetyLevel safetyLevel,
                                          List<ProductRecommendationItem> items,
                                          String notice) {}
