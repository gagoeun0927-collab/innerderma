package com.innerderma.knowledge.treatment;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;

class TreatmentKnowledgeBaseTest {

    private TreatmentKnowledgeBase kb;

    @BeforeEach
    void setUp() {
        kb = new TreatmentKnowledgeBase(new ObjectMapper());
        kb.load();
    }

    @Test
    void loadsAllTreatmentsFromJson() {
        assertThat(kb.size()).isGreaterThan(0);
    }

    @Test
    void findsByCodeCaseInsensitive() {
        assertThat(kb.findByCode("laser_toning")).isPresent();
        assertThat(kb.findByCode("LASER_TONING")).isPresent();
    }

    @Test
    void returnsEmptyForUnknownCode() {
        assertThat(kb.findByCode("NONEXISTENT")).isEmpty();
    }

    @Test
    void treatmentRuleContainsRequiredFields() {
        TreatmentRule rule = kb.findByCode("LASER_TONING").orElseThrow();
        assertThat(rule.treatmentName()).isNotBlank();
        assertThat(rule.expectedRecoveryDaysMin()).isGreaterThan(0);
        assertThat(rule.restrictedProductTags()).isNotEmpty();
        assertThat(rule.allowedProductTags()).isNotEmpty();
    }
}
