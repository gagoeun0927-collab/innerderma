package com.innerderma.dailycare.api;

import com.innerderma.carehistory.application.CareGenerationType;
import com.innerderma.carehistory.application.CarePhase;
import com.innerderma.caresolution.api.CareStepResponse;
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
                        CareGenerationType generationType, SafetyLevel safetyLevel, String headline,
                        List<CareStepResponse> steps, List<Product> products, String safetyMessage,
                        String productNotice, boolean completionRecorded, boolean completed) {
        static Phase from(DailyCarePhaseResult result) {
            var solution = result.solution();
            List<CareStepResponse> structuredSteps = new java.util.ArrayList<>();
            for (int i = 0; i < result.steps().size(); i++) {
                structuredSteps.add(CareStepResponse.fromLegacyString(result.steps().get(i), i));
            }
            return new Phase(result.phase(), solution.getCareCycle().getOriginCaptureDate(),
                    result.inherited(), CareGenerationType.of(result.inherited()),
                    solution.getSafetyLevel(), solution.getHeadline(), List.copyOf(structuredSteps),
                    result.products().stream().map(item -> new Product(ProductResponse.from(item.product()),
                            item.reason())).toList(), solution.getSafetyMessage(), result.productNotice(),
                    result.completionRecorded(), result.completed());
        }
    }

    public record Product(ProductResponse product, String reason) {}
}
