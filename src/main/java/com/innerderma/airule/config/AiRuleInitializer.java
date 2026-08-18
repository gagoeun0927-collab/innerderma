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
