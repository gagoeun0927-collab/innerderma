package com.innerderma.knowledge.product;

import com.innerderma.airule.solution.SolutionObject;
import com.innerderma.knowledge.treatment.TreatmentKnowledgeBase;
import com.innerderma.knowledge.treatment.TreatmentRule;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * Rule Engine의 SolutionObject 결과와 Knowledge Base를 조합해 적합 제품을 선택한다.
 *
 * <p>Rule Engine은 제품을 직접 생성하지 않는다. Product Matcher가 SolutionObject의
 * actions(concern, recommendation_mode, limit_new_product_addition 등)와
 * restrictions(restricted product tags)를 참조해 KB에서 필터링한다.
 *
 * <p>제품 선택 순서 (§20/§22 기준):
 * 1. Primary concern으로 후보 필터
 * 2. Treatment compatibility 필터 (시술 회복 중이면)
 * 3. Restricted product tags 제거
 * 4. Usage time(night/morning) 분리
 * 5. Step 상한 적용 (night_max_steps / morning_max_steps / inner_care_max_items)
 * 6. limit_new_product_addition이면 빈 목록(기존 루틴 유지)
 */
@Service
public class ProductMatcher {

    private final PieceSeoulKnowledgeBase pieceSeoulKb;
    private final WimStoreKnowledgeBase wimStoreKb;
    private final TreatmentKnowledgeBase treatmentKb;

    public ProductMatcher(PieceSeoulKnowledgeBase pieceSeoulKb,
                          WimStoreKnowledgeBase wimStoreKb,
                          TreatmentKnowledgeBase treatmentKb) {
        this.pieceSeoulKb = pieceSeoulKb;
        this.wimStoreKb = wimStoreKb;
        this.treatmentKb = treatmentKb;
    }

    public ProductMatchResult match(SolutionObject solution, String primaryConcern,
                                    String treatmentCode, List<String> userAllergens) {
        Map<String, Object> actions = solution.actions();
        List<String> restrictions = solution.restrictions();

        // limit_new_product_addition이 true이면 빈 목록 반환(기존 루틴 유지 원칙)
        if (Boolean.TRUE.equals(actions.get("limit_new_product_addition"))) {
            return new ProductMatchResult(List.of(), List.of(), List.of(), primaryConcern, treatmentCode);
        }

        // Step 상한
        int nightMax = getInt(actions, "night_max_steps", 4);
        int morningMax = getInt(actions, "morning_max_steps", 3);
        int innerCareMax = getInt(actions, "inner_care_max_items", 1);

        // Treatment restricted tags
        List<String> restrictedTags = getRestrictedTags(treatmentCode);

        // Piece Seoul 필터링
        List<PieceSeoulProduct> nightCandidates = pieceSeoulKb.filter(
                primaryConcern, treatmentCode, restrictedTags, "night");
        List<PieceSeoulProduct> morningCandidates = pieceSeoulKb.filter(
                primaryConcern, treatmentCode, restrictedTags, "morning");

        // WIM Store 필터링
        List<WimStoreProduct> innerCareCandidates = wimStoreKb.filter(
                primaryConcern, userAllergens, List.of());

        // Step 상한 적용
        List<PieceSeoulProduct> nightProducts = nightCandidates.stream().limit(nightMax).toList();
        List<PieceSeoulProduct> morningProducts = morningCandidates.stream().limit(morningMax).toList();
        List<WimStoreProduct> innerCareProducts = innerCareCandidates.stream().limit(innerCareMax).toList();

        return new ProductMatchResult(nightProducts, morningProducts, innerCareProducts,
                primaryConcern, treatmentCode);
    }

    private List<String> getRestrictedTags(String treatmentCode) {
        if (treatmentCode == null) return List.of();
        return treatmentKb.findByCode(treatmentCode)
                .map(TreatmentRule::restrictedProductTags)
                .orElse(List.of());
    }

    private int getInt(Map<String, Object> actions, String key, int defaultValue) {
        Object value = actions.get(key);
        if (value instanceof Number number) {
            return number.intValue();
        }
        return defaultValue;
    }
}
