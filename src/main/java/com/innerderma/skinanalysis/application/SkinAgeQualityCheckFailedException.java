package com.innerderma.skinanalysis.application;

/**
 * SkinAge 서버가 이미지 품질 검사 실패(422)를 반환했을 때 발생하는 예외.
 * 분석을 시도했지만 이미지가 요건을 충족하지 못한 정상적인 흐름이다.
 */
public class SkinAgeQualityCheckFailedException extends RuntimeException {

    private final SkinAgeQualityCheckResult qualityResult;

    public SkinAgeQualityCheckFailedException(SkinAgeQualityCheckResult qualityResult) {
        super("SkinAge quality check failed: " + qualityResult.failedChecks());
        this.qualityResult = qualityResult;
    }

    public SkinAgeQualityCheckResult getQualityResult() {
        return qualityResult;
    }
}
