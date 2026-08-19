package com.innerderma.airule.config;

import com.innerderma.airule.domain.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AiRuleInitializer {
    private static final String VERSION = "1.0.0";

    @Bean
    CommandLineRunner initializeAiRules(AiRuleRepository repository) {
        return args -> {
            // Safety Gate — requires_safety_attention(canonical signal from SelfCheck) 사용
            create(repository, "R000", AiRuleCategory.SAFETY, "Safety Attention Gate", 1000,
                    "{\"requires_safety_attention\":true}",
                    "{\"safety_status\":\"CAUTION\",\"recommendation_mode\":\"CONSERVATIVE\",\"require_professional_review_message\":true,\"limit_new_product_addition\":true}",
                    "[\"NO_AGGRESSIVE_ROUTINE\",\"MINIMIZE_PRODUCT_PROMOTION\"]",
                    "현재 상태에서는 일반 루틴보다 주의가 필요합니다. 새로운 제품 추가를 제한합니다.");

            // Image Quality Gate — image_quality_failed 신호 또는 기존 개별 플래그
            create(repository, "R002", AiRuleCategory.INPUT_IMAGE, "Image Quality Gate", 900,
                    "{\"image_quality_failed\":true,\"face_not_detected\":true,\"or_image_blurry\":true,\"or_lighting_insufficient\":true,\"or_face_partially_occluded\":true}",
                    "{\"request_retake\":true,\"confidence_policy\":\"REDUCE\"}",
                    "[\"NO_DEFINITIVE_STATE\"]", "촬영 상태를 확인한 뒤 밝은 곳에서 다시 촬영해 주세요.");

            // Minimum Intervention (기존 유지)
            create(repository, "R010", AiRuleCategory.PRIORITY_GOAL, "Minimum Intervention", 500,
                    "{\"always\":true}",
                    "{\"night_max_steps\":4,\"morning_max_steps\":3,\"inner_care_max_items\":1}",
                    "[\"NO_UNNECESSARY_PRODUCT_ADDITION\"]", "오늘 필요한 최소한의 관리만 안내합니다.");

            // Trend Rules
            create(repository, "R020", AiRuleCategory.TREND, "Trend Improving", 800,
                    "{\"trend_improving\":true}",
                    "{\"recommendation_mode\":\"MAINTENANCE\",\"no_additional_product\":true}",
                    "[\"MAINTAIN_EXISTING_ROUTINE\"]",
                    "피부 상태가 개선되고 있습니다. 기존 루틴을 유지합니다.");

            create(repository, "R021", AiRuleCategory.TREND, "Trend Worsening", 800,
                    "{\"trend_worsening\":true}",
                    "{\"recommendation_mode\":\"CONSERVATIVE\",\"require_safety_reevaluation\":true,\"limit_new_product_addition\":true}",
                    "[\"REEVALUATE_SAFETY\"]",
                    "피부 상태 변화가 감지되었습니다. 보수적인 관리를 우선합니다.");

            create(repository, "R022", AiRuleCategory.TREND, "Trend Stable", 700,
                    "{\"trend_stable\":true}",
                    "{\"recommendation_mode\":\"NORMAL\"}",
                    "[]", "피부 상태가 안정적입니다.");

            create(repository, "R023", AiRuleCategory.TREND, "Trend Unknown", 750,
                    "{\"trend_unknown\":true}",
                    "{\"recommendation_mode\":\"CONSERVATIVE\",\"limit_new_product_addition\":true}",
                    "[]", "비교할 수 있는 이전 데이터가 부족합니다. 보수적인 관리를 우선합니다.");

            // Concern Mapping (always 발화, 실제 concern은 SignalAssembler→MappedConcern에서 제공)
            create(repository, "R030", AiRuleCategory.SKIN_STATE, "Self Report Concern Mapping", 600,
                    "{\"always\":true}",
                    "{\"concern_source\":\"SELF_REPORT\"}",
                    "[]", "자가문진 기반 피부 관심사를 매핑합니다.");

            // Confidence Rule
            create(repository, "R025", AiRuleCategory.SKIN_STATE, "Low Confidence Gate", 650,
                    "{\"low_confidence\":true}",
                    "{\"recommendation_mode\":\"CONSERVATIVE\",\"confidence_policy\":\"REDUCE\",\"limit_new_product_addition\":true}",
                    "[\"NO_DEFINITIVE_STATE\"]", "분석 신뢰도가 낮습니다. 보수적인 관리를 우선합니다.");

            // === R001: 시술 후 회복기 제한 ===
            create(repository, "R001", AiRuleCategory.SAFETY, "Post-Procedure Recovery Gate", 950,
                    "{\"in_recovery_period\":true}",
                    "{\"recommendation_mode\":\"CONSERVATIVE\",\"apply_treatment_restrictions\":true,\"limit_new_product_addition\":true}",
                    "[\"NO_AGGRESSIVE_ROUTINE\",\"FOLLOW_AFTERCARE_RESTRICTIONS\"]",
                    "시술 후 회복 기간입니다. 시술 aftercare 지침을 우선 적용합니다.");

            // === R003~R005: 시술 유형별 제품 제한 ===
            create(repository, "R003", AiRuleCategory.PROCEDURE, "Laser Treatment Restriction", 850,
                    "{\"treatment_type_laser\":true}",
                    "{\"restrict_tags\":[\"retinol\",\"aha\",\"bha\",\"scrub\"],\"prioritize_tags\":[\"barrier\",\"moisturizer\",\"soothing\"]}",
                    "[\"NO_ACTIVE_INGREDIENTS_DURING_RECOVERY\"]",
                    "레이저 시술 후에는 자극 성분을 제한하고 보습/진정을 우선합니다.");

            create(repository, "R004", AiRuleCategory.PROCEDURE, "Peeling Treatment Restriction", 850,
                    "{\"treatment_type_peeling\":true}",
                    "{\"restrict_tags\":[\"retinol\",\"aha\",\"bha\",\"vitamin_c\"],\"prioritize_tags\":[\"moisturizer\",\"barrier\",\"sunscreen\"]}",
                    "[\"NO_EXFOLIATION_DURING_RECOVERY\"]",
                    "필링 시술 후에는 각질 제거 성분을 제한하고 보습/자외선 차단을 강화합니다.");

            create(repository, "R005", AiRuleCategory.PROCEDURE, "Injection Treatment Restriction", 850,
                    "{\"treatment_type_injection\":true}",
                    "{\"restrict_tags\":[\"massage\",\"pressure\"],\"prioritize_tags\":[\"soothing\",\"moisturizer\"]}",
                    "[\"NO_PRESSURE_ON_TREATMENT_AREA\"]",
                    "주사 시술 후에는 시술 부위 압박을 피하고 진정 케어를 우선합니다.");

            // === R006~R008: 증상 심각도별 대응 ===
            create(repository, "R006", AiRuleCategory.SKIN_STATE, "Severe Dryness Response", 750,
                    "{\"dominant_dryness_severe\":true}",
                    "{\"prioritize_concern\":\"HYDRATION\",\"night_max_steps\":3,\"prioritize_tags\":[\"moisturizer\",\"barrier\",\"oil\"]}",
                    "[\"HYDRATION_PRIORITY\"]",
                    "심한 건조 상태입니다. 보습 집중 케어를 우선합니다.");

            create(repository, "R007", AiRuleCategory.SKIN_STATE, "Severe Redness Response", 750,
                    "{\"dominant_redness_severe\":true}",
                    "{\"prioritize_concern\":\"REDNESS\",\"restrict_tags\":[\"retinol\",\"aha\",\"bha\"],\"prioritize_tags\":[\"soothing\",\"cica\"]}",
                    "[\"SOOTHING_PRIORITY\",\"NO_IRRITANT_PRODUCTS\"]",
                    "심한 홍조 상태입니다. 진정 케어를 우선하고 자극 성분을 제한합니다.");

            create(repository, "R008", AiRuleCategory.SKIN_STATE, "Severe Breakout Response", 750,
                    "{\"dominant_breakout_severe\":true}",
                    "{\"prioritize_concern\":\"ACNE\",\"restrict_tags\":[\"oil\",\"heavy_cream\"],\"prioritize_tags\":[\"non_comedogenic\",\"lightweight\"]}",
                    "[\"LIGHTWEIGHT_PRODUCTS_ONLY\"]",
                    "심한 트러블 상태입니다. 가벼운 제형을 우선하고 유분기 높은 제품을 제한합니다.");

            // === R011~R013: 루틴 복잡도 제한 ===
            create(repository, "R011", AiRuleCategory.PRIORITY_GOAL, "Beginner Routine Limit", 450,
                    "{\"user_experience_beginner\":true}",
                    "{\"night_max_steps\":2,\"morning_max_steps\":2,\"inner_care_max_items\":0}",
                    "[\"SIMPLE_ROUTINE_FOR_BEGINNER\"]",
                    "간단한 루틴부터 시작합니다.");

            create(repository, "R012", AiRuleCategory.PRIORITY_GOAL, "Intermediate Routine", 450,
                    "{\"user_experience_intermediate\":true}",
                    "{\"night_max_steps\":3,\"morning_max_steps\":2,\"inner_care_max_items\":1}",
                    "[]", "적절한 단계의 루틴을 구성합니다.");

            create(repository, "R013", AiRuleCategory.PRIORITY_GOAL, "Advanced Routine", 450,
                    "{\"user_experience_advanced\":true}",
                    "{\"night_max_steps\":5,\"morning_max_steps\":3,\"inner_care_max_items\":2}",
                    "[]", "충분한 경험을 바탕으로 집중 루틴을 구성합니다.");

            // === R014~R016: 계절별 조정 ===
            create(repository, "R014", AiRuleCategory.SEASON, "Summer Season", 400,
                    "{\"season_summer\":true}",
                    "{\"prioritize_tags\":[\"sunscreen\",\"lightweight\",\"oil_control\"],\"restrict_tags\":[\"heavy_cream\",\"oil\"]}",
                    "[]", "여름철에는 자외선 차단을 강화하고 가벼운 제형을 우선합니다.");

            create(repository, "R015", AiRuleCategory.SEASON, "Winter Season", 400,
                    "{\"season_winter\":true}",
                    "{\"prioritize_tags\":[\"moisturizer\",\"barrier\",\"oil\",\"rich_cream\"],\"restrict_tags\":[\"lightweight_only\"]}",
                    "[]", "겨울철에는 보습을 강화합니다.");

            create(repository, "R016", AiRuleCategory.SEASON, "Transitional Season", 400,
                    "{\"season_transitional\":true}",
                    "{\"prioritize_tags\":[\"barrier\",\"soothing\"]}",
                    "[]", "환절기에는 피부 장벽 강화와 진정을 우선합니다.");

            // === R017~R019: 연속 악화 알림 ===
            create(repository, "R017", AiRuleCategory.ALERT, "Consecutive Worsening Alert", 900,
                    "{\"consecutive_worsening_3\":true}",
                    "{\"safety_status\":\"CAUTION\",\"require_professional_review_message\":true,\"limit_new_product_addition\":true}",
                    "[\"RECOMMEND_PROFESSIONAL_CONSULTATION\"]",
                    "3회 연속 피부 상태가 악화되고 있습니다. 전문가 상담을 권장합니다.");

            create(repository, "R018", AiRuleCategory.ALERT, "No Improvement Alert", 850,
                    "{\"no_improvement_7days\":true}",
                    "{\"recommendation_mode\":\"CONSERVATIVE\",\"suggest_routine_change\":true}",
                    "[\"CONSIDER_ROUTINE_CHANGE\"]",
                    "7일간 개선이 없습니다. 루틴 변경을 고려해 보세요.");

            create(repository, "R019", AiRuleCategory.ALERT, "Rapid Deterioration Alert", 950,
                    "{\"rapid_deterioration\":true}",
                    "{\"safety_status\":\"CAUTION\",\"recommendation_mode\":\"CONSERVATIVE\",\"require_professional_review_message\":true,\"limit_new_product_addition\":true}",
                    "[\"URGENT_PROFESSIONAL_CONSULTATION\"]",
                    "급격한 피부 상태 악화가 감지되었습니다. 즉시 전문가 상담을 권장합니다.");
        };
    }

    private void create(AiRuleRepository repository, String ruleId, AiRuleCategory category,
                        String name, int priority, String conditions, String actions,
                        String restrictions, String explanation) {
        if (!repository.existsByRuleIdAndVersion(ruleId, VERSION)) {
            repository.save(new AiRule(ruleId, category, name, priority, conditions, actions,
                    restrictions, explanation, VERSION, true));
        }
    }
}
