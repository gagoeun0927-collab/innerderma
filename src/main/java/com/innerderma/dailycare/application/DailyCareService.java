package com.innerderma.dailycare.application;

import com.innerderma.caresolution.application.CareSolutionService;
import com.innerderma.carehistory.application.CarePhase;
import com.innerderma.common.error.BusinessException;
import com.innerderma.common.error.ErrorCode;
import com.innerderma.productrecommendation.application.ProductRecommendationService;
import com.innerderma.productrecommendation.application.ProductRecommendationItem;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;

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
        LocalDate servedDate = date == null ? LocalDate.now(ZoneId.of("Asia/Seoul")) : date;
        var phases = new ArrayList<DailyCarePhaseResult>(2);

        // 기상 후에는 당일 촬영 결과가 아직 없으므로 전날까지의 최신 솔루션을 사용한다.
        try {
            var morningSolution = careSolutionService.getDaily(userCode, servedDate.minusDays(1));
            var morningProducts = productRecommendationService.getDaily(userCode, servedDate.minusDays(1));
            phases.add(new DailyCarePhaseResult(CarePhase.MORNING, servedDate, true,
                    morningSolution.solution(), morningSolution.morningSteps(),
                    filterProducts(morningProducts.items(), CarePhase.MORNING), morningProducts.notice()));
        } catch (BusinessException exception) {
            if (exception.errorCode() != ErrorCode.CARE_SOLUTION_NOT_FOUND) throw exception;
        }

        var eveningSolution = careSolutionService.getDaily(userCode, servedDate);
        var eveningProducts = productRecommendationService.getDaily(userCode, servedDate);
        phases.add(new DailyCarePhaseResult(CarePhase.EVENING, servedDate,
                eveningSolution.inherited(), eveningSolution.solution(), eveningSolution.eveningSteps(),
                filterProducts(eveningProducts.items(), CarePhase.EVENING), eveningProducts.notice()));
        return new DailyCareResult(servedDate, java.util.List.copyOf(phases));
    }

    private java.util.List<ProductRecommendationItem> filterProducts(
            java.util.List<ProductRecommendationItem> items, CarePhase phase) {
        return items.stream().filter(item -> item.usagePhase().equals("EVENING_AND_MORNING")
                        || item.usagePhase().equals(phase.name()))
                .toList();
    }
}
