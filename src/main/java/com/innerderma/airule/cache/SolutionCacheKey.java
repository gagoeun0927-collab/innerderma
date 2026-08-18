package com.innerderma.airule.cache;

import java.time.LocalDate;

/**
 * Solution 캐시 키. 같은 사용자 + 같은 날짜 + 같은 scoringVersion + 같은 ruleVersion이면
 * Solution을 재계산하지 않는다 (§33 멱등성).
 */
public record SolutionCacheKey(
        String userCode,
        LocalDate date,
        String scoringVersion,
        String ruleVersion
) {
    public static SolutionCacheKey of(String userCode, LocalDate date,
                                      String scoringVersion, String ruleVersion) {
        return new SolutionCacheKey(userCode, date, scoringVersion, ruleVersion);
    }
}
