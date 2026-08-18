package com.innerderma.airule.golden;

import java.util.List;
import java.util.Map;

/**
 * golden-tests/*.json 파일 1개의 Java 매핑.
 */
public record GoldenTestCase(
        String id,
        String category,
        String description,
        Input input,
        Expected expected
) {
    public record Input(Map<String, Boolean> signals) {}

    public record Expected(
            List<String> fired_rules,
            List<String> not_fired_rules,
            Map<String, Object> actions_contain,
            List<String> restrictions_contain
    ) {}
}
