package com.innerderma.dailycare.application;

import com.innerderma.carehistory.application.CarePhase;
import com.innerderma.caresolution.domain.CareSolution;
import com.innerderma.productrecommendation.application.ProductRecommendationItem;

import java.time.LocalDate;
import java.util.List;

public record DailyCarePhaseResult(CarePhase phase, LocalDate servedDate, boolean inherited,
                                   CareSolution solution, List<String> steps,
                                   List<ProductRecommendationItem> products, String productNotice) {}
