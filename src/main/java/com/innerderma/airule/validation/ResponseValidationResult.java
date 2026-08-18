package com.innerderma.airule.validation;

import java.util.List;

/**
 * LLM 응답 검증 결과. 하나라도 violation이 있으면 valid=false.
 */
public record ResponseValidationResult(boolean valid, List<String> violations) {

    public static ResponseValidationResult success() {
        return new ResponseValidationResult(true, List.of());
    }

    public static ResponseValidationResult fail(List<String> violations) {
        return new ResponseValidationResult(false, List.copyOf(violations));
    }
}
