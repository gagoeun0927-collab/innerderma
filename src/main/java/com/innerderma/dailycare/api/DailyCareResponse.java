package com.innerderma.dailycare.api;

import com.innerderma.caresolution.api.CareSolutionResponse;
import com.innerderma.dailycare.application.DailyCareResult;
import com.innerderma.productrecommendation.api.ProductRecommendationResponse;

public record DailyCareResponse(CareSolutionResponse careSolution,
                                ProductRecommendationResponse productRecommendations) {
    public static DailyCareResponse from(DailyCareResult result) {
        return new DailyCareResponse(CareSolutionResponse.from(result.solution()),
                ProductRecommendationResponse.from(result.productRecommendations()));
    }
}
