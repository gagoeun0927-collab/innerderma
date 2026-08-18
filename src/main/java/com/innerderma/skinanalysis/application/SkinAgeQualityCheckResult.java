package com.innerderma.skinanalysis.application;

import java.util.List;

/**
 * SkinAge 서버가 이미지 품질 검사를 실패로 판정했을 때의 결과.
 * failed_checks 는 SkinAge 서버가 반환한 원본 값(occlusion, face_angle, blur)이며,
 * Rule Engine 신호로의 매핑은 SignalAssembler 에서 수행한다.
 */
public record SkinAgeQualityCheckResult(List<String> failedChecks, List<String> messages) {

    public boolean hasFailed(String check) {
        return failedChecks != null && failedChecks.contains(check);
    }
}
