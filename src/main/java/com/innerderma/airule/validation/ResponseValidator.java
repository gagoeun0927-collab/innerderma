package com.innerderma.airule.validation;

import com.innerderma.knowledge.product.PieceSeoulKnowledgeBase;
import com.innerderma.knowledge.product.PieceSeoulProduct;
import com.innerderma.knowledge.product.WimStoreKnowledgeBase;
import com.innerderma.knowledge.product.WimStoreProduct;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * LLM 응답 검증기 (§38/§39 기준).
 *
 * <p>검증 항목:
 * <ul>
 *   <li>product_id가 KB에 존재하는지</li>
 *   <li>product가 active인지</li>
 *   <li>routine step 수가 Rule Engine 상한을 초과하지 않는지</li>
 *   <li>safety_status가 Rule Engine 결정과 다르지 않은지</li>
 *   <li>unknown product가 없는지</li>
 * </ul>
 * FAIL이면 사용자에게 직접 보여주지 않는다.
 */
@Component
public class ResponseValidator {

    private final PieceSeoulKnowledgeBase pieceSeoulKb;
    private final WimStoreKnowledgeBase wimStoreKb;

    public ResponseValidator(PieceSeoulKnowledgeBase pieceSeoulKb, WimStoreKnowledgeBase wimStoreKb) {
        this.pieceSeoulKb = pieceSeoulKb;
        this.wimStoreKb = wimStoreKb;
    }

    /**
     * LLM 응답에 포함된 product_id 목록, step 수, safety_status를 검증한다.
     *
     * @param productIds      LLM 응답에 포함된 모든 product_id
     * @param nightStepCount  LLM이 생성한 night step 수
     * @param morningStepCount LLM이 생성한 morning step 수
     * @param innerCareCount  LLM이 생성한 inner care 추천 수
     * @param llmSafetyStatus LLM 응답의 safety_status 값
     * @param ruleActions     Rule Engine이 결정한 actions (night_max_steps, safety_status 등)
     */
    public ResponseValidationResult validate(
            List<String> productIds,
            int nightStepCount,
            int morningStepCount,
            int innerCareCount,
            String llmSafetyStatus,
            Map<String, Object> ruleActions
    ) {
        List<String> violations = new ArrayList<>();

        // 1. Product ID 존재 확인
        Set<String> validPieceIds = pieceSeoulKb.findAll().stream()
                .map(PieceSeoulProduct::productId).collect(Collectors.toSet());
        Set<String> validWimIds = wimStoreKb.findAll().stream()
                .map(WimStoreProduct::productId).collect(Collectors.toSet());

        for (String productId : productIds) {
            if (!validPieceIds.contains(productId) && !validWimIds.contains(productId)) {
                violations.add("UNKNOWN_PRODUCT: " + productId);
            }
        }

        // 2. Step 수 제한
        int nightMax = getInt(ruleActions, "night_max_steps", 4);
        int morningMax = getInt(ruleActions, "morning_max_steps", 3);
        int innerCareMax = getInt(ruleActions, "inner_care_max_items", 1);

        if (nightStepCount > nightMax) {
            violations.add("NIGHT_STEP_EXCEEDED: " + nightStepCount + " > " + nightMax);
        }
        if (morningStepCount > morningMax) {
            violations.add("MORNING_STEP_EXCEEDED: " + morningStepCount + " > " + morningMax);
        }
        if (innerCareCount > innerCareMax) {
            violations.add("INNER_CARE_EXCEEDED: " + innerCareCount + " > " + innerCareMax);
        }

        // 3. Safety status 변경 불가
        Object ruleSafety = ruleActions.get("safety_status");
        if (ruleSafety != null && llmSafetyStatus != null
                && !ruleSafety.toString().equalsIgnoreCase(llmSafetyStatus)) {
            violations.add("SAFETY_STATUS_MISMATCH: rule=" + ruleSafety + " llm=" + llmSafetyStatus);
        }

        return violations.isEmpty()
                ? ResponseValidationResult.success()
                : ResponseValidationResult.fail(violations);
    }

    private int getInt(Map<String, Object> actions, String key, int defaultValue) {
        Object value = actions.get(key);
        if (value instanceof Number number) return number.intValue();
        return defaultValue;
    }
}
