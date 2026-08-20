package com.innerderma.common.demo;

import com.innerderma.skinstate.domain.SkinStateSnapshotRepository;
import com.innerderma.skinstate.trend.SkinStateTrend;
import com.innerderma.skinstate.trend.SkinStateTrendService;
import com.innerderma.user.domain.User;
import com.innerderma.user.domain.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 시연용 자동 시딩이 Trend를 산출 가능한 상태로 만드는지 검증한다.
 *
 * <p>회귀 방지 대상: 스냅샷이 1개면 trend_unknown → R023 발화 →
 * limit_new_product_addition → 제품 추천이 빈 목록이 된다.
 * 즉 신규 사용자가 항상 빈 루틴을 받는 문제가 재발하는지 잡는다.
 */
@SpringBootTest
@Transactional
class DemoUserDataSeederTest {

    @Autowired
    private DemoUserDataSeeder seeder;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SkinStateSnapshotRepository snapshotRepository;

    @Autowired
    private SkinStateTrendService trendService;

    @Test
    void seedsTwoSnapshotsSoTrendIsComputable() {
        String userCode = "SEED-TEST-001";
        userRepository.save(new User(userCode, "시더 테스트", "010-1111-2222"));

        boolean seeded = seeder.ensureMinimumData(userCode);

        assertThat(seeded).isTrue();
        assertThat(snapshotRepository.findTop2ByUser_UserCodeOrderBySnapshotDateDesc(userCode))
                .hasSize(2);

        // 스냅샷이 2개이므로 trend가 UNKNOWN이 아니어야 한다 (R023 발화 방지)
        var trend = trendService.evaluateLatest(userCode);
        assertThat(trend.overallTrend()).isNotEqualTo(SkinStateTrend.UNKNOWN);
        // 어제 점수 합계(7) > 오늘(4) → 개선
        assertThat(trend.overallTrend()).isEqualTo(SkinStateTrend.IMPROVING);
        assertThat(trend.toRuleSignals().get("trend_unknown")).isFalse();
    }

    @Test
    void doesNotOverwriteExistingData() {
        String userCode = "SEED-TEST-002";
        userRepository.save(new User(userCode, "시더 테스트2", "010-3333-4444"));

        seeder.ensureMinimumData(userCode);
        int afterFirst = snapshotRepository
                .findByUser_UserCodeAndSnapshotDateAfterOrderBySnapshotDateDesc(
                        userCode, java.time.LocalDate.now().minusDays(30)).size();

        boolean seededAgain = seeder.ensureMinimumData(userCode);

        assertThat(seededAgain).isFalse();
        int afterSecond = snapshotRepository
                .findByUser_UserCodeAndSnapshotDateAfterOrderBySnapshotDateDesc(
                        userCode, java.time.LocalDate.now().minusDays(30)).size();
        assertThat(afterSecond).isEqualTo(afterFirst);
    }

    @Test
    void doesNotCreateSafetyAttentionSignal() {
        String userCode = "SEED-TEST-003";
        userRepository.save(new User(userCode, "시더 테스트3", "010-5555-6666"));

        seeder.ensureMinimumData(userCode);

        // 자동 생성 문진은 안전 주의 신호를 만들지 않아야 한다
        // (근거 없는 위험 상태를 임의로 만들지 않는다는 원칙)
        var snapshot = snapshotRepository
                .findFirstByUser_UserCodeOrderBySnapshotDateDesc(userCode).orElseThrow();
        assertThat(snapshot.getDominantSymptom()).isEqualTo("dryness");
        assertThat(snapshot.getSymptomScoresJson()).doesNotContain("\"pain\":2");
        assertThat(snapshot.getSymptomScoresJson()).doesNotContain("\"pain\":3");
    }
}
