package com.innerderma.common.demo;

import com.innerderma.selfcheck.application.SelfCheckCommand;
import com.innerderma.selfcheck.application.SelfCheckService;
import com.innerderma.selfcheck.domain.SelfCheck;
import com.innerderma.selfcheck.domain.SelfCheckRepository;
import com.innerderma.selfcheck.domain.SymptomSeverity;
import com.innerderma.skinstate.application.SkinStateSnapshotService;
import com.innerderma.skinstate.domain.SkinStateSnapshot;
import com.innerderma.skinstate.domain.SkinStateSnapshotRepository;
import com.innerderma.user.domain.User;
import com.innerderma.user.domain.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 시연용: 자가문진/스냅샷 데이터가 없는 신규 사용자에게 최소 데이터를 자동 생성한다.
 *
 * <p><b>스냅샷을 2개 만드는 이유:</b> Trend Engine은 같은 scoringVersion의 스냅샷 2개를 비교해야
 * 방향을 판정한다. 1개뿐이면 {@code trend_unknown}이 되고, 그러면 R023(Trend Unknown)이 발화해
 * {@code limit_new_product_addition=true}가 적용되어 <b>제품 추천이 빈 목록으로 반환된다.</b>
 * 즉 신규 사용자는 항상 빈 루틴을 받게 되므로, 어제/오늘 2건을 만들어 트렌드가 산출되게 한다.
 *
 * <p>어제 점수를 오늘보다 높게(나쁘게) 두어 합계 delta가 음수가 되고 {@code trend_improving}이
 * 산출된다. 이러면 제품을 막는 규칙(R000/R001/R017/R019/R021/R023/R025)이 발화하지 않는다.
 *
 * <p>기존 서비스를 최대한 재사용해 생성된 데이터가 자가문진·스냅샷·트렌드 화면에서도 일관되게 보인다.
 * 다만 어제 데이터는 서비스가 {@code now()}로 시각을 고정하므로 엔티티를 직접 저장한다.
 *
 * <p><b>주의:</b> 시연 편의용이다. 실제 서비스에서는
 * {@code innerderma.demo.auto-seed-user-data=false}로 끄고 사용자가 직접 문진을 입력하게 해야 한다.
 * 자동 생성 데이터는 note의 {@code [demo]} 표식으로 구분한다.
 */
@Component
public class DemoUserDataSeeder {

    private static final org.slf4j.Logger log =
            org.slf4j.LoggerFactory.getLogger(DemoUserDataSeeder.class);

    /** 자동 생성된 데이터임을 식별하기 위한 표식. */
    public static final String DEMO_NOTE = "[demo] 신규 사용자 자동 생성 문진";
    public static final String DEMO_NOTE_PREVIOUS = "[demo] 신규 사용자 자동 생성 문진 (이전 기록)";

    private final UserRepository userRepository;
    private final SelfCheckRepository selfCheckRepository;
    private final SelfCheckService selfCheckService;
    private final SkinStateSnapshotRepository snapshotRepository;
    private final SkinStateSnapshotService snapshotService;
    private final ObjectMapper objectMapper;
    private final boolean enabled;

    public DemoUserDataSeeder(UserRepository userRepository,
                              SelfCheckRepository selfCheckRepository,
                              SelfCheckService selfCheckService,
                              SkinStateSnapshotRepository snapshotRepository,
                              SkinStateSnapshotService snapshotService,
                              ObjectMapper objectMapper,
                              @Value("${innerderma.demo.auto-seed-user-data:true}") boolean enabled) {
        this.userRepository = userRepository;
        this.selfCheckRepository = selfCheckRepository;
        this.selfCheckService = selfCheckService;
        this.snapshotRepository = snapshotRepository;
        this.snapshotService = snapshotService;
        this.objectMapper = objectMapper;
        this.enabled = enabled;
    }

    /**
     * 자가문진/스냅샷이 없으면 시연용 기본 데이터를 만든다.
     * 이미 데이터가 있으면 아무것도 하지 않는다(사용자 실제 입력을 덮어쓰지 않음).
     *
     * @return 새로 생성했으면 true
     */
    @Transactional
    public boolean ensureMinimumData(String userCode) {
        if (!enabled) {
            return false;
        }

        boolean hasSelfCheck = selfCheckRepository
                .findFirstByUser_UserCodeOrderByCheckedAtDesc(userCode).isPresent();
        boolean hasSnapshot = snapshotRepository
                .findFirstByUser_UserCodeOrderBySnapshotDateDesc(userCode).isPresent();

        if (hasSelfCheck && hasSnapshot) {
            return false;
        }

        User user = userRepository.findByUserCode(userCode).orElse(null);
        if (user == null) {
            return false;
        }

        if (!hasSelfCheck) {
            // 1) 어제 기록 — 트렌드 비교 대상. 서비스는 now()로 고정되므로 엔티티를 직접 저장한다.
            seedPreviousDay(user);

            // 2) 오늘 기록 — 기존 서비스 재사용 (다른 화면과 동일한 경로)
            selfCheckService.create(userCode, todaySelfCheck());
            log.info("Demo auto-seed: created 2 self-checks (yesterday + today) for new user {}", userCode);
        }

        // 3) 오늘 스냅샷 생성 (최신 자가문진 기준)
        snapshotService.refreshFromLatestSelfCheck(userCode);
        log.info("Demo auto-seed: created skin state snapshot for user {}", userCode);

        return true;
    }

    /**
     * 어제 자가문진 + 스냅샷을 직접 저장한다.
     * 오늘보다 증상 점수 합계를 높게 두어 trend가 improving으로 산출되게 한다.
     */
    private void seedPreviousDay(User user) {
        LocalDate yesterday = LocalDate.now().minusDays(1);
        if (snapshotRepository.findByUser_UserCodeAndSnapshotDate(user.getUserCode(), yesterday).isPresent()) {
            return;
        }

        SelfCheckCommand previous = previousDaySelfCheck();
        SelfCheck selfCheck = selfCheckRepository.save(new SelfCheck(
                user,
                LocalDateTime.of(yesterday, LocalTime.of(21, 0)),
                previous.pain(), previous.heatSensation(), previous.tightness(), previous.dryness(),
                previous.itching(), previous.swelling(), previous.peeling(), previous.breakout(),
                previous.oozing(), previous.bleeding(), previous.barrierDamage(), previous.note()
        ));

        Map<String, Integer> scores = ordinalScores(previous);
        snapshotRepository.save(new SkinStateSnapshot(
                user,
                yesterday,
                SkinStateSnapshotService.SCORING_VERSION,
                objectMapper.writeValueAsString(scores),
                null,
                dominantSymptom(scores),
                selfCheck.getId(),
                null,
                LocalDateTime.of(yesterday, LocalTime.of(21, 0))
        ));
    }

    /**
     * 오늘 자가문진 값.
     *
     * <p>dryness=MODERATE를 최고점으로 두어 dominantSymptom이 dryness가 되고,
     * 보습(HYDRATION) 중심 추천이 나온다. 가장 일반적인 피부 고민이라 시연에 적합하다.
     * 증상 점수 합계 = 4 (tightness 1 + dryness 2 + peeling 1).
     *
     * <p>안전 신호는 의도적으로 발생시키지 않는다. pain/heatSensation/swelling은 MODERATE 이상이면,
     * oozing/bleeding은 MILD만으로도 안전 주의로 해석되므로 모두 NONE으로 둔다.
     * 근거 없는 위험 상태를 임의로 만들지 않기 위함이다.
     */
    private SelfCheckCommand todaySelfCheck() {
        return new SelfCheckCommand(
                SymptomSeverity.NONE,      // pain
                SymptomSeverity.NONE,      // heatSensation
                SymptomSeverity.MILD,      // tightness
                SymptomSeverity.MODERATE,  // dryness ← dominant
                SymptomSeverity.NONE,      // itching
                SymptomSeverity.NONE,      // swelling
                SymptomSeverity.MILD,      // peeling
                SymptomSeverity.NONE,      // breakout
                SymptomSeverity.NONE,      // oozing
                SymptomSeverity.NONE,      // bleeding
                SymptomSeverity.MILD,      // barrierDamage
                DEMO_NOTE
        );
    }

    /**
     * 어제 자가문진 값. 증상 점수 합계 = 7 (tightness 2 + dryness 2 + itching 1 + peeling 2).
     * 오늘(4)보다 높아 delta가 음수 → trend_improving.
     * 안전 신호를 유발하는 축(pain/heatSensation/swelling/oozing/bleeding)은 여기서도 NONE.
     */
    private SelfCheckCommand previousDaySelfCheck() {
        return new SelfCheckCommand(
                SymptomSeverity.NONE,      // pain
                SymptomSeverity.NONE,      // heatSensation
                SymptomSeverity.MODERATE,  // tightness
                SymptomSeverity.MODERATE,  // dryness
                SymptomSeverity.MILD,      // itching
                SymptomSeverity.NONE,      // swelling
                SymptomSeverity.MODERATE,  // peeling
                SymptomSeverity.NONE,      // breakout
                SymptomSeverity.NONE,      // oozing
                SymptomSeverity.NONE,      // bleeding
                SymptomSeverity.MILD,      // barrierDamage
                DEMO_NOTE_PREVIOUS
        );
    }

    /** SkinStateSnapshotService와 동일한 순서 보존 인코딩 (NONE=0 … SEVERE=3). */
    private Map<String, Integer> ordinalScores(SelfCheckCommand command) {
        Map<String, Integer> scores = new LinkedHashMap<>();
        scores.put("pain", ordinal(command.pain()));
        scores.put("heatSensation", ordinal(command.heatSensation()));
        scores.put("tightness", ordinal(command.tightness()));
        scores.put("dryness", ordinal(command.dryness()));
        scores.put("itching", ordinal(command.itching()));
        scores.put("swelling", ordinal(command.swelling()));
        scores.put("peeling", ordinal(command.peeling()));
        scores.put("breakout", ordinal(command.breakout()));
        return scores;
    }

    private int ordinal(SymptomSeverity severity) {
        return switch (severity) {
            case NONE -> 0;
            case MILD -> 1;
            case MODERATE -> 2;
            case SEVERE -> 3;
        };
    }

    /** 최고점 축. 동점 시 AXIS_ORDER 순서로 결정 (SkinStateSnapshotService와 동일 규칙). */
    private String dominantSymptom(Map<String, Integer> scores) {
        String dominant = null;
        int best = 0;
        for (String axis : SkinStateSnapshotService.AXIS_ORDER) {
            int score = scores.getOrDefault(axis, 0);
            if (score > best) {
                best = score;
                dominant = axis;
            }
        }
        return dominant;
    }
}
