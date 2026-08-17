package com.innerderma.dailycare.application;

import com.innerderma.caresolution.application.*;
import com.innerderma.productrecommendation.application.*;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class DailyCareServiceTest {
    @Test
    void usesSameServedDateForSolutionAndProducts() {
        CareSolutionService solutionService = mock(CareSolutionService.class);
        ProductRecommendationService recommendationService = mock(ProductRecommendationService.class);
        DailyCareService service = new DailyCareService(solutionService, recommendationService);
        LocalDate date = LocalDate.of(2026, 8, 20);
        CareSolutionResult solution = mock(CareSolutionResult.class);
        ProductRecommendationResult recommendations = mock(ProductRecommendationResult.class);
        when(solution.servedDate()).thenReturn(date);
        when(solutionService.getDaily("WHS-DEMO-001", date)).thenReturn(solution);
        when(recommendationService.getDaily("WHS-DEMO-001", date)).thenReturn(recommendations);

        DailyCareResult result = service.getDaily("WHS-DEMO-001", date);

        assertThat(result.solution()).isSameAs(solution);
        assertThat(result.productRecommendations()).isSameAs(recommendations);
    }
}
