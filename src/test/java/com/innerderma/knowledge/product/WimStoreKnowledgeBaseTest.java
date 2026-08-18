package com.innerderma.knowledge.product;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class WimStoreKnowledgeBaseTest {

    private WimStoreKnowledgeBase kb;

    @BeforeEach
    void setUp() {
        kb = new WimStoreKnowledgeBase(new ObjectMapper());
        kb.load();
    }

    @Test
    void loadsActiveProductsFromJson() {
        assertThat(kb.size()).isGreaterThan(0);
    }

    @Test
    void filtersByConcern() {
        List<WimStoreProduct> results = kb.findByConcern("BARRIER_RECOVERY");
        assertThat(results).isNotEmpty();
    }

    @Test
    void excludesProductsWithUserAllergens() {
        List<WimStoreProduct> all = kb.findAll();
        List<WimStoreProduct> filtered = kb.filter(null, List.of("milk", "soy"), List.of());
        // milk 또는 soy 알레르겐이 있는 제품이 제외돼야 함
        assertThat(filtered.size()).isLessThanOrEqualTo(all.size());
        assertThat(filtered).noneMatch(p -> p.hasAllergen(List.of("milk", "soy")));
    }

    @Test
    void returnsAllWhenNoAllergens() {
        List<WimStoreProduct> filtered = kb.filter(null, List.of(), List.of());
        assertThat(filtered).hasSize(kb.size());
    }
}
