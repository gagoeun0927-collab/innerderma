package com.innerderma.dailycare.application;

import com.innerderma.caresolution.application.CareSolutionService;
import com.innerderma.productrecommendation.application.ProductRecommendationService;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
public class DailyCareService {
    private final CareSolutionService careSolutionService;
    private final ProductRecommendationService productRecommendationService;

    public DailyCareService(CareSolutionService careSolutionService,
                            ProductRecommendationService productRecommendationService) {
        this.careSolutionService = careSolutionService;
        this.productRecommendationService = productRecommendationService;
    }

    public DailyCareResult getDaily(String userCode, LocalDate date) {
        var solution = careSolutionService.getDaily(userCode, date);
        var recommendations = productRecommendationService.getDaily(userCode, solution.servedDate());
        return new DailyCareResult(solution, recommendations);
    }
}
