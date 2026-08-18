package com.innerderma.airule.cache;

import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory Solution 캐시 (§33/§34).
 *
 * <p>같은 사용자 + 날짜 + scoringVersion + ruleVersion이면 기존 Solution을 반환한다.
 * 새로운 분석·문진·시술 정보가 등록되면 해당 사용자의 캐시를 무효화한다.
 * DB 저장 없이 in-memory ConcurrentHashMap으로 구현하며, 앱 재시작 시 초기화된다.
 *
 * <p>재생성 조건 (§35):
 * - 새로운 얼굴 분석 결과
 * - 사용자가 문진 수정
 * - 새로운 시술 정보 등록
 * - Rule/Product version 변경
 * - Safety status 변경
 *
 * 이 조건들은 각 서비스에서 {@link #invalidate(String)}을 호출해 처리한다.
 */
@Component
public class SolutionCache {

    private final ConcurrentHashMap<SolutionCacheKey, SolutionCacheEntry> cache = new ConcurrentHashMap<>();

    public Optional<SolutionCacheEntry> get(SolutionCacheKey key) {
        return Optional.ofNullable(cache.get(key));
    }

    public void put(SolutionCacheKey key, SolutionCacheEntry entry) {
        cache.put(key, entry);
    }

    /** 특정 사용자의 모든 캐시를 무효화한다. */
    public void invalidate(String userCode) {
        cache.keySet().removeIf(key -> key.userCode().equals(userCode));
    }

    /** 전체 캐시를 비운다. Rule/Product version 변경 시 사용. */
    public void invalidateAll() {
        cache.clear();
    }

    public int size() {
        return cache.size();
    }
}
