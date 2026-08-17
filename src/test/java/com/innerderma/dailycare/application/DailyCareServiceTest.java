package com.innerderma.dailycare.application;

import com.innerderma.caresolution.application.*;
import com.innerderma.productrecommendation.application.*;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class DailyCareServiceTest {
    @Test
    void usesPreviousSolutionForMorningAndCurrentSolutionForEvening() {
        CareSolutionService solutionService = mock(CareSolutionService.class);
        ProductRecommendationService recommendationService = mock(ProductRecommendationService.class);
        DailyCareService service = new DailyCareService(solutionService, recommendationService);
        LocalDate date = LocalDate.of(2026, 8, 20);
        CareSolutionResult morningSolution = mock(CareSolutionResult.class);
        CareSolutionResult eveningSolution = mock(CareSolutionResult.class);
        ProductRecommendationResult morningProducts = mock(ProductRecommendationResult.class);
        ProductRecommendationResult eveningProducts = mock(ProductRecommendationResult.class);
        when(morningSolution.morningSteps()).thenReturn(List.of("아침 단계"));
        when(eveningSolution.eveningSteps()).thenReturn(List.of("저녁 단계"));
        when(morningProducts.items()).thenReturn(List.of());
        when(eveningProducts.items()).thenReturn(List.of());
        when(solutionService.getDaily("WHS-DEMO-001", date.minusDays(1))).thenReturn(morningSolution);
        when(solutionService.getDaily("WHS-DEMO-001", date)).thenReturn(eveningSolution);
        when(recommendationService.getDaily("WHS-DEMO-001", date.minusDays(1))).thenReturn(morningProducts);
        when(recommendationService.getDaily("WHS-DEMO-001", date)).thenReturn(eveningProducts);

        DailyCareResult result = service.getDaily("WHS-DEMO-001", date);

        assertThat(result.phases()).extracting(DailyCarePhaseResult::phase)
                .containsExactly(com.innerderma.carehistory.application.CarePhase.MORNING,
                        com.innerderma.carehistory.application.CarePhase.EVENING);
        assertThat(result.phases().get(0).steps()).containsExactly("아침 단계");
        assertThat(result.phases().get(0).inherited()).isTrue();
        assertThat(result.phases().get(1).steps()).containsExactly("저녁 단계");
    }
}
