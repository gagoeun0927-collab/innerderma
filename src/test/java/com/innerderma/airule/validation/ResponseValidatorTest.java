package com.innerderma.airule.validation;

import com.innerderma.knowledge.product.PieceSeoulKnowledgeBase;
import com.innerderma.knowledge.product.WimStoreKnowledgeBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ResponseValidatorTest {

    private ResponseValidator validator;

    @BeforeEach
    void setUp() {
        ObjectMapper om = new ObjectMapper();
        PieceSeoulKnowledgeBase pieceKb = new PieceSeoulKnowledgeBase(om);
        pieceKb.load();
        WimStoreKnowledgeBase wimKb = new WimStoreKnowledgeBase(om);
        wimKb.load();
        validator = new ResponseValidator(pieceKb, wimKb);
    }

    @Test
    void passesWhenAllProductsExistAndLimitsRespected() {
        ResponseValidationResult result = validator.validate(
                List.of("PSS_001", "WIM_001"), 2, 1, 1, "NORMAL",
                Map.of("night_max_steps", 4, "morning_max_steps", 3, "inner_care_max_items", 1));

        assertThat(result.valid()).isTrue();
        assertThat(result.violations()).isEmpty();
    }

    @Test
    void failsForUnknownProductId() {
        ResponseValidationResult result = validator.validate(
                List.of("FAKE_999"), 1, 1, 0, null, Map.of());

        assertThat(result.valid()).isFalse();
        assertThat(result.violations()).anyMatch(v -> v.contains("UNKNOWN_PRODUCT"));
    }

    @Test
    void failsWhenNightStepsExceedLimit() {
        ResponseValidationResult result = validator.validate(
                List.of(), 5, 1, 0, null, Map.of("night_max_steps", 4));

        assertThat(result.valid()).isFalse();
        assertThat(result.violations()).anyMatch(v -> v.contains("NIGHT_STEP_EXCEEDED"));
    }

    @Test
    void failsWhenSafetyStatusMismatch() {
        ResponseValidationResult result = validator.validate(
                List.of(), 1, 1, 0, "NORMAL",
                Map.of("safety_status", "CAUTION"));

        assertThat(result.valid()).isFalse();
        assertThat(result.violations()).anyMatch(v -> v.contains("SAFETY_STATUS_MISMATCH"));
    }
}
