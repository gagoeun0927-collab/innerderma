package com.innerderma.airule.cache;

import com.innerderma.airule.solution.SolutionObject;
import com.innerderma.knowledge.product.ProductMatchResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class SolutionCacheTest {

    private SolutionCache cache;

    @BeforeEach
    void setUp() {
        cache = new SolutionCache();
    }

    private SolutionCacheKey key(String user, LocalDate date) {
        return SolutionCacheKey.of(user, date, "selfcheck-ordinal-v1", "1.0.0");
    }

    private SolutionCacheEntry entry() {
        return new SolutionCacheEntry(
                new SolutionObject(Map.of(), List.of(), List.of(), List.of(), Map.of(), Map.of()),
                new ProductMatchResult(List.of(), List.of(), List.of(), null, null),
                LocalDateTime.now());
    }

    @Test
    void returnsEmptyForMissingKey() {
        assertThat(cache.get(key("user1", LocalDate.of(2026, 8, 18)))).isEmpty();
    }

    @Test
    void storesAndRetrievesEntry() {
        SolutionCacheKey k = key("user1", LocalDate.of(2026, 8, 18));
        cache.put(k, entry());

        assertThat(cache.get(k)).isPresent();
        assertThat(cache.size()).isEqualTo(1);
    }

    @Test
    void invalidateRemovesUserEntries() {
        cache.put(key("user1", LocalDate.of(2026, 8, 18)), entry());
        cache.put(key("user2", LocalDate.of(2026, 8, 18)), entry());

        cache.invalidate("user1");

        assertThat(cache.get(key("user1", LocalDate.of(2026, 8, 18)))).isEmpty();
        assertThat(cache.get(key("user2", LocalDate.of(2026, 8, 18)))).isPresent();
    }

    @Test
    void invalidateAllClearsEverything() {
        cache.put(key("user1", LocalDate.of(2026, 8, 18)), entry());
        cache.put(key("user2", LocalDate.of(2026, 8, 18)), entry());

        cache.invalidateAll();

        assertThat(cache.size()).isZero();
    }
}
