package com.innerderma.caresolution.application;

import com.innerderma.caresolution.domain.CareSolution;

import java.time.LocalDate;
import java.util.List;

public record CareSolutionResult(CareSolution solution, List<String> eveningSteps,
                                 List<String> morningSteps, LocalDate servedDate) {
    public boolean inherited() {
        return servedDate.isAfter(solution.getCareCycle().getOriginCaptureDate());
    }
}
