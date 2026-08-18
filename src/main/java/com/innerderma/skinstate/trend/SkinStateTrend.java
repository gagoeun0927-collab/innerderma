package com.innerderma.skinstate.trend;

/**
 * 스냅샷 간 결정적 변화 비교값(R005). 임상 진단이 아니다.
 * 각 값은 Rule Engine 신호 이름으로 변환된다.
 */
public enum SkinStateTrend {
    IMPROVING("trend_improving"),
    STABLE("trend_stable"),
    WORSENING("trend_worsening"),
    UNKNOWN("trend_unknown");

    private final String signalName;

    SkinStateTrend(String signalName) {
        this.signalName = signalName;
    }

    public String signalName() {
        return signalName;
    }
}
