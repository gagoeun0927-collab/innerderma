package com.innerderma.common.demo;

import com.innerderma.selfcheck.application.SelfCheckCommand;
import com.innerderma.selfcheck.application.SelfCheckService;
import com.innerderma.selfcheck.domain.SelfCheckRepository;
import com.innerderma.selfcheck.domain.SymptomSeverity;
import com.innerderma.skinstate.application.SkinStateSnapshotService;
import com.innerderma.skinstate.domain.SkinStateSnapshotRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 시연용: 자가문진/스냅샷 데이터가 없는 신규 사용자에게 최소 데이터를 자동 생성한다.
 *
 * <p>AI 파이프라인(SignalAssembler → RuleEngine)은 SkinStateSnapshot을 신호 원천으로 사용하므로,
 * 데이터가 전혀 없는 사용자는 신호가 거의 비어 있어 의미 있는 추천이 나오지 않는다.
 * 시연 중 신규 사용자가 바로 화면을 볼 수 있도록, 기존 서비스를 그대로 사용해
 * 자가문진 1건 + 스냅샷 1건을 만든다.
 *
 * <p>기존 서비스를 재사용하므로 생성된 데이터는 자가문진 조회·스냅샷·트렌드 화면에서도
 * 일관되게 보인다. 직접 엔티티를 만들지 않는 이유도 이 일관성 때문이다.
 *
 * <p><b>주의:</b> 이 클래스는 시연 편의를 위한 것이다. 실제 서비스에서는
 * {@code innerderma.demo.auto-seed-user-data=false}로 끄고, 사용자가 직접 자가문진을 입력하게 해야 한다.
 * 임의로 만든 문진 값을 실제 사용자 데이터로 오인하지 않도록 note에 표식을 남긴다.
 */
@Component
public class DemoUserDataSeeder {

    private static final org.slf4j.Logger log =
            org.slf4j.LoggerFactory.getLogger(DemoUserDataSeeder.class);

    /** 자동 생성된 데이터임을 식별하기 위한 표식. */
    public static final String DEMO_NOTE = "[demo] 신규 사용자 자동 생성 문진";

    private final SelfCheckRepository selfCheckRepository;
    private final SelfCheckService selfCheckService;
    private final SkinStateSnapshotRepository snapshotRepository;
    private final SkinStateSnapshotService snapshotService;
    private final boolean enabled;

    public DemoUserDataSeeder(SelfCheckRepository selfCheckRepository,
                              SelfCheckService selfCheckService,
                              SkinStateSnapshotRepository snapshotRepository,
                              SkinStateSnapshotService snapshotService,
                              @Value("${innerderma.demo.auto-seed-user-data:true}") boolean enabled) {
        this.selfCheckRepository = selfCheckRepository;
        this.selfCheckService = selfCheckService;
        this.snapshotRepository = snapshotRepository;
        this.snapshotService = snapshotService;
        this.enabled = enabled;
    }

    /**
     * 자가문진/스냅샷이 없으면 시연용 기본 데이터를 만든다.
     * 이미 데이터가 있으면 아무것도 하지 않는다(사용자 실제 입력을 덮어쓰지 않음).
     *
     * @return 새로 생성했으면 true
     */
    public boolean ensureMinimumData(String userCode) {
        if (!enabled) {
            return false;
        }

        boolean seeded = false;

        if (selfCheckRepository.findFirstByUser_UserCodeOrderByCheckedAtDesc(userCode).isEmpty()) {
            selfCheckService.create(userCode, defaultSelfCheck());
            seeded = true;
            log.info("Demo auto-seed: created default self-check for new user {}", userCode);
        }

        if (snapshotRepository.findFirstByUser_UserCodeOrderBySnapshotDateDesc(userCode).isEmpty()) {
            snapshotService.refreshFromLatestSelfCheck(userCode);
            seeded = true;
            log.info("Demo auto-seed: created skin state snapshot for new user {}", userCode);
        }

        return seeded;
    }

    /**
     * 시연용 기본 자가문진 값.
     *
     * <p>dryness=MODERATE를 최고점으로 두어 dominantSymptom이 dryness가 되고,
     * 보습(HYDRATION) 중심 추천이 나오게 한다. 가장 일반적인 피부 고민이라 시연에 적합하다.
     *
     * <p>안전 신호는 의도적으로 발생시키지 않는다. pain/heatSensation/swelling은
     * MODERATE 이상이면 안전 주의로 해석되고, oozing/bleeding은 MILD만으로도 주의 신호가 되므로
     * 모두 NONE으로 둔다. 근거 없는 위험 상태를 임의로 만들지 않기 위함이다.
     */
    private SelfCheckCommand defaultSelfCheck() {
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
}
