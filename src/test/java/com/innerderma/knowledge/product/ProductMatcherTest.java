package com.innerderma.knowledge.product;

import com.innerderma.airule.solution.SolutionObject;
import com.innerderma.knowledge.treatment.TreatmentKnowledgeBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class ProductMatcherTest {

    private ProductMatcher matcher;

    @BeforeEach
    void setUp() {
        ObjectMapper om = new ObjectMapper();
        PieceSeoulKnowledgeBase pieceKb = new PieceSeoulKnowledgeBase(om);
        pieceKb.load();
        WimStoreKnowledgeBase wimKb = new WimStoreKnowledgeBase(om);
        wimKb.load();
        TreatmentKnowledgeBase treatmentKb = new TreatmentKnowledgeBase(om);
        treatmentKb.load();
        matcher = new ProductMatcher(pieceKb, wimKb, treatmentKb, mock(com.innerderma.knowledge.product.usage.ProductRecommendationLogRepository.class));
    }

    @Test
    void matchesProductsForBarrierRecoveryConcern() {
        SolutionObject solution = new SolutionObject(
                Map.of("night_max_steps", 4, "morning_max_steps", 3, "inner_care_max_items", 1),
                List.of(), List.of(), List.of(), Map.of(), Map.of());

        ProductMatchResult result = matcher.match(solution, "BARRIER_RECOVERY", null, List.of());

        assertThat(result.nightProducts()).isNotEmpty();
        assertThat(result.nightProducts().size()).isLessThanOrEqualTo(4);
        assertThat(result.innerCareProducts().size()).isLessThanOrEqualTo(1);
    }

    @Test
    void respectsStepLimits() {
        SolutionObject solution = new SolutionObject(
                Map.of("night_max_steps", 2, "morning_max_steps", 1, "inner_care_max_items", 1),
                List.of(), List.of(), List.of(), Map.of(), Map.of());

        ProductMatchResult result = matcher.match(solution, "HYDRATION", null, List.of());

        assertThat(result.nightProducts().size()).isLessThanOrEqualTo(2);
        assertThat(result.morningProducts().size()).isLessThanOrEqualTo(1);
    }

    @Test
    void returnsEmptyWhenLimitNewProductAddition() {
        SolutionObject solution = new SolutionObject(
                Map.of("limit_new_product_addition", true, "night_max_steps", 4),
                List.of(), List.of(), List.of(), Map.of(), Map.of());

        ProductMatchResult result = matcher.match(solution, "REDNESS", null, List.of());

        assertThat(result.nightProducts()).isEmpty();
        assertThat(result.morningProducts()).isEmpty();
        assertThat(result.innerCareProducts()).isEmpty();
    }

    @Test
    void filtersRestrictedTagsFromTreatmentKb() {
        SolutionObject solution = new SolutionObject(
                Map.of("night_max_steps", 10), List.of(), List.of(), List.of(), Map.of(), Map.of());

        ProductMatchResult result = matcher.match(solution, "BARRIER_RECOVERY", "LASER_TONING", List.of());

        // 레이저 토닝 후에는 retinol 등 restricted_product_tags 제품이 제외돼야 함
        assertThat(result.nightProducts()).allSatisfy(p ->
                assertThat(p.tags()).noneMatch(tag ->
                        tag.equalsIgnoreCase("retinol") || tag.equalsIgnoreCase("AHA") || tag.equalsIgnoreCase("BHA")));
    }

    @Test
    void excludesWimProductsWithUserAllergens() {
        SolutionObject solution = new SolutionObject(
                Map.of("inner_care_max_items", 10), List.of(), List.of(), List.of(), Map.of(), Map.of());

        ProductMatchResult withAllergen = matcher.match(solution, null, null, List.of("milk"));
        ProductMatchResult withoutAllergen = matcher.match(solution, null, null, List.of());

        assertThat(withAllergen.innerCareProducts().size())
                .isLessThanOrEqualTo(withoutAllergen.innerCareProducts().size());
    }
}
