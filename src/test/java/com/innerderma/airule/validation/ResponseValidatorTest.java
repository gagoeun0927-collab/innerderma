package com.innerderma.airule.validation;

import com.innerderma.knowledge.product.PieceSeoulKnowledgeBase;
import com.innerderma.knowledge.product.PieceSeoulProduct;
import com.innerderma.knowledge.product.WimStoreKnowledgeBase;
import com.innerderma.knowledge.product.WimStoreProduct;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ResponseValidatorTest {

    private ResponseValidator validator;
    private PieceSeoulKnowledgeBase pieceSeoulKb;
    private WimStoreKnowledgeBase wimStoreKb;

    @BeforeEach
    void setUp() {
        pieceSeoulKb = mock(PieceSeoulKnowledgeBase.class);
        wimStoreKb = mock(WimStoreKnowledgeBase.class);
        validator = new ResponseValidator(pieceSeoulKb, wimStoreKb);

        when(pieceSeoulKb.findAll()).thenReturn(List.of(
                pieceProduct("PSS_001", true),
                pieceProduct("PSS_002", true),
                pieceProduct("PSS_INACTIVE", false)
        ));
        when(wimStoreKb.findAll()).thenReturn(List.of(
                wimProduct("WIM_001", true),
                wimProduct("WIM_INACTIVE", false)
        ));
    }

    @Test
    void successWhenAllProductsValidAndWithinLimits() {
        var result = validator.validate(
                List.of("PSS_001", "WIM_001"), 2, 1, 1, "NORMAL",
                Map.of("night_max_steps", 4, "morning_max_steps", 3, "inner_care_max_items", 1),
                "오늘의 케어", "수분 부족");

        assertThat(result.valid()).isTrue();
        assertThat(result.violations()).isEmpty();
    }

    @Test
    void failsWhenUnknownProductDetected() {
        var result = validator.validate(
                List.of("UNKNOWN_PROD"), 1, 0, 0, "NORMAL",
                Map.of(), "headline", "summary");

        assertThat(result.valid()).isFalse();
        assertThat(result.violations()).anyMatch(v -> v.contains("UNKNOWN_PRODUCT"));
    }

    @Test
    void failsWhenInactiveProductDetected() {
        var result = validator.validate(
                List.of("PSS_INACTIVE"), 1, 0, 0, "NORMAL",
                Map.of(), "headline", "summary");

        assertThat(result.valid()).isFalse();
        assertThat(result.violations()).anyMatch(v -> v.contains("INACTIVE_PRODUCT"));
    }

    @Test
    void failsWhenNightStepsExceedLimit() {
        var result = validator.validate(
                List.of("PSS_001"), 5, 0, 0, "NORMAL",
                Map.of("night_max_steps", 4), "headline", "summary");

        assertThat(result.valid()).isFalse();
        assertThat(result.violations()).anyMatch(v -> v.contains("NIGHT_STEP_EXCEEDED"));
    }

    @Test
    void failsWhenMorningStepsExceedLimit() {
        var result = validator.validate(
                List.of("PSS_001"), 0, 4, 0, "NORMAL",
                Map.of("morning_max_steps", 3), "headline", "summary");

        assertThat(result.valid()).isFalse();
        assertThat(result.violations()).anyMatch(v -> v.contains("MORNING_STEP_EXCEEDED"));
    }

    @Test
    void failsWhenInnerCareExceedsLimit() {
        var result = validator.validate(
                List.of("WIM_001"), 0, 0, 3, "NORMAL",
                Map.of("inner_care_max_items", 1), "headline", "summary");

        assertThat(result.valid()).isFalse();
        assertThat(result.violations()).anyMatch(v -> v.contains("INNER_CARE_EXCEEDED"));
    }

    @Test
    void failsWhenSafetyStatusMismatch() {
        var result = validator.validate(
                List.of(), 0, 0, 0, "NORMAL",
                Map.of("safety_status", "CAUTION"), "headline", "summary");

        assertThat(result.valid()).isFalse();
        assertThat(result.violations()).anyMatch(v -> v.contains("SAFETY_STATUS_MISMATCH"));
    }

    @Test
    void failsWhenHeadlineIsBlank() {
        var result = validator.validate(
                List.of(), 0, 0, 0, "NORMAL", Map.of(), "  ", "summary");

        assertThat(result.valid()).isFalse();
        assertThat(result.violations()).contains("EMPTY_HEADLINE");
    }

    @Test
    void failsWhenSkinStateSummaryIsBlank() {
        var result = validator.validate(
                List.of(), 0, 0, 0, "NORMAL", Map.of(), "headline", "");

        assertThat(result.valid()).isFalse();
        assertThat(result.violations()).contains("EMPTY_SKIN_STATE_SUMMARY");
    }

    @Test
    void sixParamOverloadDelegatesCorrectly() {
        var result = validator.validate(
                List.of("PSS_001"), 1, 0, 0, "NORMAL", Map.of());

        assertThat(result.valid()).isTrue();
    }

    private PieceSeoulProduct pieceProduct(String id, boolean active) {
        return new PieceSeoulProduct(id, "Brand", "Name", "MOISTURIZER",
                List.of(), List.of(), List.of(), List.of(),
                List.of("night"), "daily", "1ml", "도포",
                List.of(), List.of(), List.of(), List.of(), active, 10000, null, null, null);
    }

    private WimStoreProduct wimProduct(String id, boolean active) {
        return new WimStoreProduct(id, "Brand", "Name", "JELLY",
                List.of(), List.of(), List.of(), List.of(),
                "1일 1포", List.of(), List.of(), List.of(), active, 20000, null, null, null);
    }
}
