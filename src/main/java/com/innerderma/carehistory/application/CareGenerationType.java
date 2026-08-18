package com.innerderma.carehistory.application;

/**
 * 캘린더 표기용 케어 생성 유형. 당일 새 촬영으로 생성된 솔루션인지, 이전 솔루션을 승계한 것인지를 구분한다.
 */
public enum CareGenerationType {
    /** 당일 유효 촬영으로 새 분석과 솔루션이 생성됨. */
    NEW_ANALYSIS,
    /** 새 촬영이 없어 가장 최근 솔루션을 현재 사이클에 이어서 제공함. */
    CARRIED_FORWARD;

    public static CareGenerationType of(boolean inherited) {
        return inherited ? CARRIED_FORWARD : NEW_ANALYSIS;
    }
}
