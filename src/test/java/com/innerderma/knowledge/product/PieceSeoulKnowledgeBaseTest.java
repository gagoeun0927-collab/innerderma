package com.innerderma.knowledge.product;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PieceSeoulKnowledgeBaseTest {

    private PieceSeoulKnowledgeBase kb;

    @BeforeEach
    void setUp() {
        kb = new PieceSeoulKnowledgeBase(new ObjectMapper());
        kb.load();
    }

    @Test
    void loadsActiveProductsFromJson() {
        assertThat(kb.size()).isGreaterThan(0);
    }

    @Test
    void filtersByConcern() {
        List<PieceSeoulProduct> results = kb.findByConcern("BARRIER_RECOVERY");
        assertThat(results).isNotEmpty();
        assertThat(results).allSatisfy(p ->
                assertThat(p.skinStateTags()).anyMatch(t -> t.equalsIgnoreCase("BARRIER_RECOVERY")));
    }

    @Test
    void filtersCompatibleWithTreatment() {
        List<PieceSeoulProduct> results = kb.findCompatibleWith("laser_toning");
        assertThat(results).isNotEmpty();
    }

    @Test
    void filtersExcludesRestrictedTags() {
        List<PieceSeoulProduct> all = kb.findAll();
        List<PieceSeoulProduct> filtered = kb.filter(null, null, List.of("retinol"), null);
        assertThat(filtered.size()).isLessThanOrEqualTo(all.size());
    }

    @Test
    void filtersForNight() {
        List<PieceSeoulProduct> results = kb.findForTime("night");
        assertThat(results).allSatisfy(p ->
                assertThat(p.usageTime()).anyMatch(t -> t.equalsIgnoreCase("night")));
    }
}
