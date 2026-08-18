package com.innerderma.knowledge.treatment;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.annotation.PostConstruct;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * JSON 파일 기반 Treatment Knowledge Base.
 * 시술 코드로 회복 규칙을 조회한다. DB 스키마 변경 없이 JSON에서 로딩한다.
 */
@Component
public class TreatmentKnowledgeBase {

    private static final String DATA_PATH = "knowledge/treatment_knowledge_base.json";

    private final ObjectMapper objectMapper;
    private Map<String, TreatmentRule> rules = Collections.emptyMap();

    public TreatmentKnowledgeBase(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    public void load() {
        try {
            InputStream is = new ClassPathResource(DATA_PATH).getInputStream();
            List<TreatmentRuleJson> items = objectMapper.readValue(is,
                    objectMapper.getTypeFactory().constructCollectionType(List.class, TreatmentRuleJson.class));
            Map<String, TreatmentRule> map = new LinkedHashMap<>();
            for (TreatmentRuleJson item : items) {
                TreatmentRule rule = item.toRule();
                map.put(rule.treatmentCode().toUpperCase(), rule);
            }
            this.rules = Collections.unmodifiableMap(map);
        } catch (IOException exception) {
            // 파일 없으면 빈 KB로 동작
            this.rules = Collections.emptyMap();
        }
    }

    public Optional<TreatmentRule> findByCode(String treatmentCode) {
        if (treatmentCode == null) return Optional.empty();
        return Optional.ofNullable(rules.get(treatmentCode.toUpperCase()));
    }

    public List<TreatmentRule> findAll() {
        return List.copyOf(rules.values());
    }

    public int size() {
        return rules.size();
    }

    /** JSON 파일의 snake_case 구조를 매핑하는 내부 DTO */
    private static class TreatmentRuleJson {
        @JsonProperty("treatment_code") String treatmentCode;
        @JsonProperty("treatment_name") String treatmentName;
        @JsonProperty("treatment_name_en") String treatmentNameEn;
        @JsonProperty("treatment_type") String treatmentType;
        @JsonProperty("treatment_area") List<String> treatmentArea;
        @JsonProperty("expected_recovery_days_min") int expectedRecoveryDaysMin;
        @JsonProperty("expected_recovery_days_max") int expectedRecoveryDaysMax;
        @JsonProperty("normal_symptoms") List<String> normalSymptoms;
        @JsonProperty("warning_symptoms") List<String> warningSymptoms;
        @JsonProperty("aftercare_restrictions") List<String> aftercareRestrictions;
        @JsonProperty("allowed_product_tags") List<String> allowedProductTags;
        @JsonProperty("restricted_product_tags") List<String> restrictedProductTags;
        @JsonProperty("aftercare_guide") String aftercareGuide;
        String source;
        String version;

        TreatmentRule toRule() {
            return new TreatmentRule(treatmentCode, treatmentName, treatmentNameEn, treatmentType,
                    treatmentArea != null ? treatmentArea : List.of(),
                    expectedRecoveryDaysMin, expectedRecoveryDaysMax,
                    normalSymptoms != null ? normalSymptoms : List.of(),
                    warningSymptoms != null ? warningSymptoms : List.of(),
                    aftercareRestrictions != null ? aftercareRestrictions : List.of(),
                    allowedProductTags != null ? allowedProductTags : List.of(),
                    restrictedProductTags != null ? restrictedProductTags : List.of(),
                    aftercareGuide, source, version);
        }
    }
}
