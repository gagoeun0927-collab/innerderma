package com.innerderma.caresolution.api;

import com.innerderma.carehistory.application.CareGenerationType;
import com.innerderma.caresolution.application.CareSolutionResult;
import com.innerderma.caresolution.domain.CareSeason;
import com.innerderma.caresolution.domain.SafetyLevel;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public record CareSolutionResponse(
        Long id, Long careCycleId, LocalDate originCaptureDate, LocalDate servedDate,
        boolean inherited, CareGenerationType generationType, CareSeason season, SafetyLevel safetyLevel,
        String headline, String primaryConcern,
        List<CareStepResponse> eveningSteps, List<CareStepResponse> morningSteps,
        List<String> eveningAvoid, List<String> morningAvoid,
        List<SupplementResponse> supplements,
        List<String> concernTags,
        EveningWashResponse eveningWash,
        String safetyMessage, String whsDiagnosisSummary, String procedureName,
        String procedureCareGuide, LocalDateTime generatedAt
) {
    public static CareSolutionResponse from(CareSolutionResult result) {
        var solution = result.solution();
        var diagnosis = solution.getWhsDiagnosis();
        var procedure = solution.getProcedureRecord();
        boolean attention = solution.getSafetyLevel() == SafetyLevel.ATTENTION;
        String concern = solution.getPrimaryConcern();

        return new CareSolutionResponse(solution.getId(), solution.getCareCycle().getId(),
                solution.getCareCycle().getOriginCaptureDate(), result.servedDate(), result.inherited(),
                CareGenerationType.of(result.inherited()),
                solution.getSeason(), solution.getSafetyLevel(), solution.getHeadline(),
                concern,
                toStepResponses(result.eveningSteps()),
                toStepResponses(result.morningSteps()),
                buildEveningAvoid(attention, procedure),
                buildMorningAvoid(attention),
                buildSupplements(concern),
                buildConcernTags(concern, attention),
                buildEveningWash(attention),
                solution.getSafetyMessage(), diagnosis == null ? null : diagnosis.getResultSummary(),
                procedure == null ? null : procedure.getProcedureName(),
                procedure == null ? null : procedure.getCareGuide(), solution.getGeneratedAt());
    }

    private static List<CareStepResponse> toStepResponses(List<String> steps) {
        if (steps == null) return List.of();
        List<CareStepResponse> result = new ArrayList<>();
        for (int i = 0; i < steps.size(); i++) {
            result.add(CareStepResponse.fromLegacyString(steps.get(i), i));
        }
        return List.copyOf(result);
    }

    private static List<String> buildEveningAvoid(boolean attention, com.innerderma.procedure.domain.ProcedureRecord procedure) {
        List<String> avoid = new ArrayList<>();
        if (attention) {
            avoid.add("강한 각질 제거 제품");
            avoid.add("알코올 함량이 높은 토너");
            avoid.add("자극적인 향료 성분");
            avoid.add("레티놀·고함량 비타민C");
        } else {
            avoid.add("강한 각질 제거 제품");
            avoid.add("알코올 함량이 높은 토너");
        }
        if (procedure != null) {
            avoid.add("시술 부위에 물리적 마찰을 주는 제품");
        }
        return List.copyOf(avoid);
    }

    private static List<String> buildMorningAvoid(boolean attention) {
        List<String> avoid = new ArrayList<>();
        if (attention) {
            avoid.add("밀폐력 강한 오일 선크림");
            avoid.add("두꺼운 파운데이션 제품");
            avoid.add("합성 향료·향수 성분");
        } else {
            avoid.add("밀폐력 강한 오일 선크림 (민감 시)");
            avoid.add("합성 향료·향수 성분");
        }
        return List.copyOf(avoid);
    }

    private static List<SupplementResponse> buildSupplements(String concern) {
        List<SupplementResponse> supplements = new ArrayList<>();
        if (concern != null) {
            String lower = concern.toLowerCase();
            if (lower.contains("hydration") || lower.contains("barrier") || lower.contains("dryness")) {
                supplements.add(new SupplementResponse("WIM 마린 콜라겐 앰플", "하루 1포, 아침 식후 물 또는 음료에 혼합해 섭취하세요."));
            }
            if (lower.contains("pigmentation") || lower.contains("redness")) {
                supplements.add(new SupplementResponse("WIM 글루타치온 래디언스 젤리", "하루 1포, 아침 공복 또는 식후 섭취하세요."));
            }
            if (lower.contains("acne") || lower.contains("sebum")) {
                supplements.add(new SupplementResponse("WIM 비포밀 식이섬유 스틱", "하루 1~2포, 식사 10~20분 전 충분한 물과 함께 섭취하세요."));
            }
        }
        if (supplements.isEmpty()) {
            supplements.add(new SupplementResponse("WIM 프로틴 쉐이크", "하루 1포, 물 또는 우유에 혼합해 섭취하세요."));
        }
        return List.copyOf(supplements);
    }

    private static List<String> buildConcernTags(String concern, boolean attention) {
        List<String> tags = new ArrayList<>();
        if (attention) {
            tags.add("주의 필요");
        }
        if (concern != null) {
            String lower = concern.toLowerCase();
            if (lower.contains("hydration") || lower.contains("dryness")) tags.add("수분 부족");
            if (lower.contains("barrier")) tags.add("피부 장벽 회복");
            if (lower.contains("redness")) tags.add("약한 붉어짐");
            if (lower.contains("pigmentation")) tags.add("색소 침착");
            if (lower.contains("acne")) tags.add("트러블 관리");
            if (lower.contains("wrinkle")) tags.add("주름 관리");
        }
        if (tags.isEmpty()) tags.add("피부 안정");
        if (tags.size() < 2) tags.add("일상 관리");
        return List.copyOf(tags.subList(0, Math.min(tags.size(), 3)));
    }

    private static EveningWashResponse buildEveningWash(boolean attention) {
        if (attention) {
            return new EveningWashResponse(
                    "저녁 세안 루틴",
                    "노폐물 제거",
                    "오늘은 피부가 민감한 상태입니다. 순한 클렌징 폼만으로 가볍게 세안하세요.",
                    "미온수로 30초 이내 빠르게 헹궈주세요. 문지르지 마세요.",
                    "1 펌프 · 약산성 클렌저 사용"
            );
        }
        return new EveningWashResponse(
                "저녁 세안 루틴",
                "노폐물 제거",
                "기본적으로 클렌징폼은 약산성을, 선크림이나 메이크업과 함께 외출하신 경우에는 클렌징오일을 사용해 주세요.",
                "오일은 첫 펌프 시 꼭 물이 묻지 않은 손으로, 충분한 마사지 후 깨끗이 씻어내 주세요.",
                "3~4 펌프 · 깨끗한 맨손으로 사용"
        );
    }
}
