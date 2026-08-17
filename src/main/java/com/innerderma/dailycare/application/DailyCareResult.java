package com.innerderma.dailycare.application;

import com.innerderma.caresolution.application.CareSolutionResult;
import com.innerderma.productrecommendation.application.ProductRecommendationResult;

public record DailyCareResult(CareSolutionResult solution,
                              ProductRecommendationResult productRecommendations) {}
