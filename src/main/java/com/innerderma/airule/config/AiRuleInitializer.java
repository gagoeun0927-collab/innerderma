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
            create(repository, "R000", AiRuleCategory.SAFETY, "Safety First", 1000,
                    "{\"severe_or_unusual_symptom\":true,\"or_rapidly_worsening\":true,\"or_professional_review_required\":true}",
                    "{\"safety_status\":\"CAUTION\",\"recommendation_mode\":\"CAUTION\"}",
                    "[\"NO_AGGRESSIVE_ROUTINE\",\"MINIMIZE_PRODUCT_PROMOTION\",\"REQUIRE_PROFESSIONAL_GUIDANCE\"]",
                    "증상이 지속되거나 악화되는 경우 전문가 확인이 필요합니다.");
            create(repository, "R002", AiRuleCategory.INPUT_IMAGE, "Image Quality Gate", 900,
                    "{\"face_not_detected\":true,\"or_image_blurry\":true,\"or_lighting_insufficient\":true,\"or_face_partially_occluded\":true}",
                    "{\"request_retake\":true,\"confidence_policy\":\"REDUCE\"}",
                    "[\"NO_DEFINITIVE_STATE\"]", "촬영 상태를 확인한 뒤 밝은 곳에서 다시 촬영해 주세요.");
            create(repository, "R010", AiRuleCategory.PRIORITY_GOAL, "Minimum Intervention", 500,
                    "{\"always\":true}",
                    "{\"night_max_steps\":4,\"morning_max_steps\":3,\"inner_care_max_items\":1}",
                    "[\"NO_UNNECESSARY_PRODUCT_ADDITION\"]", "오늘 필요한 최소한의 관리만 안내합니다.");
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
