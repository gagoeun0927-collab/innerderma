package com.innerderma.knowledge.treatment;

import java.util.List;

/**
 * Treatment Knowledge Base의 단일 시술 회복 규칙.
 * docs/treatment_knowledge_base.json 에서 로딩된다.
 */
public record TreatmentRule(
        String treatmentCode,
        String treatmentName,
        String treatmentNameEn,
        String treatmentType,
        List<String> treatmentArea,
        int expectedRecoveryDaysMin,
        int expectedRecoveryDaysMax,
        List<String> normalSymptoms,
        List<String> warningSymptoms,
        List<String> aftercareRestrictions,
        List<String> allowedProductTags,
        List<String> restrictedProductTags,
        String aftercareGuide,
        String source,
        String version
) {}
