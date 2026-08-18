package com.innerderma.llm;

import com.innerderma.airule.solution.SolutionObject;
import com.innerderma.knowledge.product.PieceSeoulProduct;
import com.innerderma.knowledge.product.ProductMatchResult;
import com.innerderma.knowledge.product.WimStoreProduct;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class OpenAiLlmRendererTest {

    @Test
    void returnsFallbackResponseWhenApiKeyMissing() {
        OpenAiLlmRenderer renderer = new OpenAiLlmRenderer("", new ObjectMapper(), java.net.http.HttpClient.newHttpClient());

        PieceSeoulProduct nightProduct = new PieceSeoulProduct(
                "PSS_001", "Piece Seoul", "Cica Cream", "MOISTURIZER",
                List.of("barrier"), List.of("BARRIER_RECOVERY"), List.of(), List.of(),
                List.of("night"), "daily", "fingertip", "얼굴 전체 도포",
                List.of(), List.of("장벽 강화"), List.of(), List.of(), true, 38000, null, null);
        WimStoreProduct innerProduct = new WimStoreProduct(
                "WIM_001", "WIM", "콜라겐 젤리", "JELLY",
                List.of("HYDRATION"), List.of(), List.of(), List.of(),
                "1일 1포", List.of("콜라겐 함유"), List.of(), List.of(), true, 45000, null, null);

        SolutionObject solution = new SolutionObject(
                Map.of("night_max_steps", 4), List.of(), List.of("R010@1.0.0"), List.of(), Map.of(), Map.of());
        ProductMatchResult products = new ProductMatchResult(
                List.of(nightProduct), List.of(), List.of(innerProduct), "BARRIER_RECOVERY", null);

        LlmResponse response = renderer.render(solution, products, "ko");

        assertThat(response).isNotNull();
        assertThat(response.night().steps()).hasSize(1);
        assertThat(response.night().steps().get(0).productId()).isEqualTo("PSS_001");
        assertThat(response.innerCare().recommended()).hasSize(1);
        assertThat(response.skinStateSummary()).isEqualTo("BARRIER_RECOVERY");
    }

    @Test
    void fallbackIncludesCautionWhenSafetyStatusPresent() {
        OpenAiLlmRenderer renderer = new OpenAiLlmRenderer("", new ObjectMapper(), java.net.http.HttpClient.newHttpClient());

        SolutionObject solution = new SolutionObject(
                Map.of("safety_status", "CAUTION"), List.of(), List.of(), List.of(), Map.of(), Map.of());
        ProductMatchResult products = new ProductMatchResult(List.of(), List.of(), List.of(), null, null);

        LlmResponse response = renderer.render(solution, products, "en");

        assertThat(response.caution()).isNotNull();
    }
}
