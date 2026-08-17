package com.innerderma.dailycare.api;

import com.innerderma.carehistory.application.CarePhase;
import com.innerderma.caresolution.domain.SafetyLevel;
import com.innerderma.dailycare.application.*;
import com.innerderma.product.api.ProductResponse;

import java.time.LocalDate;
import java.util.List;

public record DailyCareResponse(LocalDate servedDate, List<Phase> phases) {
    public static DailyCareResponse from(DailyCareResult result) {
        return new DailyCareResponse(result.servedDate(), result.phases().stream().map(Phase::from).toList());
    }

    public record Phase(CarePhase phase, LocalDate originCaptureDate, boolean inherited,
                        SafetyLevel safetyLevel, String headline, List<String> steps,
                        List<Product> products, String safetyMessage, String productNotice) {
        static Phase from(DailyCarePhaseResult result) {
            var solution = result.solution();
            return new Phase(result.phase(), solution.getCareCycle().getOriginCaptureDate(),
                    result.inherited(), solution.getSafetyLevel(), solution.getHeadline(), result.steps(),
                    result.products().stream().map(item -> new Product(ProductResponse.from(item.product()),
                            item.reason())).toList(), solution.getSafetyMessage(), result.productNotice());
        }
    }

    public record Product(ProductResponse product, String reason) {}
}
