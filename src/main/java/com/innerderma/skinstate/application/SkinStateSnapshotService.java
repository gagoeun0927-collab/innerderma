package com.innerderma.skinstate.application;

import com.innerderma.common.error.BusinessException;
import com.innerderma.common.error.ErrorCode;
import com.innerderma.selfcheck.domain.SelfCheck;
import com.innerderma.selfcheck.domain.SelfCheckRepository;
import com.innerderma.selfcheck.domain.SymptomSeverity;
import com.innerderma.skinstate.domain.SkinStateSnapshot;
import com.innerderma.skinstate.domain.SkinStateSnapshotRepository;
import com.innerderma.user.domain.User;
import com.innerderma.user.domain.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 자가문진 원천 기반 피부 상태 스냅샷 서비스 (MVP).
 *
 * <p>결정적·설명 가능한 기술 점수화만 수행한다. 자가문진 8개 항목을 원천 축으로 보존하고,
 * SymptomSeverity 를 순서 보존 정수(NONE=0, MILD=1, MODERATE=2, SEVERE=3)로 인코딩한다.
 * 이는 임상 중증도 점수가 아니라 단순 순서 인코딩이며, 축 간 변환·결합·R003 taxonomy 매핑은 하지 않는다.
 * dominantSymptom 은 최고점 축이며 동점 시 아래 고정 선언 순서(AXIS_ORDER)로 결정한다.
 */
@Service
@Transactional(readOnly = true)
public class SkinStateSnapshotService {

    /** 점수 산정 규약 버전. 산정 방식을 바꾸면 이 값을 올린다. */
    public static final String SCORING_VERSION = "selfcheck-ordinal-v1";

    private static final ZoneId MVP_ZONE = ZoneId.of("Asia/Seoul");

    /** 원천 축과 동점 시 tie-break 순서(자가문진 선언 순서 그대로). Trend 등 다른 계층에서 표준 축 집합으로 재사용한다. */
    public static final List<String> AXIS_ORDER = List.of(
            "pain", "heatSensation", "tightness", "dryness",
            "itching", "swelling", "peeling", "breakout");

    private final SkinStateSnapshotRepository snapshotRepository;
    private final SelfCheckRepository selfCheckRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    @Autowired
    public SkinStateSnapshotService(SkinStateSnapshotRepository snapshotRepository,
                                    SelfCheckRepository selfCheckRepository,
                                    UserRepository userRepository,
                                    ObjectMapper objectMapper) {
        this(snapshotRepository, selfCheckRepository, userRepository, objectMapper, Clock.system(MVP_ZONE));
    }

    SkinStateSnapshotService(SkinStateSnapshotRepository snapshotRepository,
                             SelfCheckRepository selfCheckRepository,
                             UserRepository userRepository,
                             ObjectMapper objectMapper, Clock clock) {
        this.snapshotRepository = snapshotRepository;
        this.selfCheckRepository = selfCheckRepository;
        this.userRepository = userRepository;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    @Transactional
    public SkinStateSnapshotResult refreshFromLatestSelfCheck(String userCode) {
        User user = userRepository.findByUserCode(userCode)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        SelfCheck selfCheck = selfCheckRepository.findFirstByUser_UserCodeOrderByCheckedAtDesc(userCode)
                .orElseThrow(() -> new BusinessException(ErrorCode.SELF_CHECK_NOT_FOUND));

        Map<String, Integer> scores = scoreSelfCheck(selfCheck);
        String dominant = dominantSymptom(scores);
        String scoresJson = writeScores(scores);
        LocalDate snapshotDate = selfCheck.getCheckedAt().toLocalDate();
        LocalDateTime now = LocalDateTime.now(clock);

        SkinStateSnapshot snapshot = snapshotRepository
                .findByUser_UserCodeAndSnapshotDate(userCode, snapshotDate)
                .map(existing -> {
                    existing.applyScoring(SCORING_VERSION, scoresJson, dominant,
                            selfCheck.getId(), null, now);
                    return existing;
                })
                .orElseGet(() -> snapshotRepository.save(new SkinStateSnapshot(
                        user, snapshotDate, SCORING_VERSION, scoresJson, dominant,
                        selfCheck.getId(), null, now)));

        return new SkinStateSnapshotResult(snapshot, scores);
    }

    public SkinStateSnapshotResult getLatest(String userCode) {
        if (!userRepository.existsByUserCode(userCode)) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        SkinStateSnapshot snapshot = snapshotRepository
                .findFirstByUser_UserCodeOrderBySnapshotDateDesc(userCode)
                .orElseThrow(() -> new BusinessException(ErrorCode.SKIN_STATE_SNAPSHOT_NOT_FOUND));
        return new SkinStateSnapshotResult(snapshot, readScores(snapshot.getSymptomScoresJson()));
    }

    private Map<String, Integer> scoreSelfCheck(SelfCheck selfCheck) {
        Map<String, Integer> scores = new LinkedHashMap<>();
        scores.put("pain", ordinalScore(selfCheck.getPain()));
        scores.put("heatSensation", ordinalScore(selfCheck.getHeatSensation()));
        scores.put("tightness", ordinalScore(selfCheck.getTightness()));
        scores.put("dryness", ordinalScore(selfCheck.getDryness()));
        scores.put("itching", ordinalScore(selfCheck.getItching()));
        scores.put("swelling", ordinalScore(selfCheck.getSwelling()));
        scores.put("peeling", ordinalScore(selfCheck.getPeeling()));
        scores.put("breakout", ordinalScore(selfCheck.getBreakout()));
        return scores;
    }

    /** 순서 보존 정수. 임상 중증도 점수가 아니다. */
    private int ordinalScore(SymptomSeverity severity) {
        return switch (severity) {
            case NONE -> 0;
            case MILD -> 1;
            case MODERATE -> 2;
            case SEVERE -> 3;
        };
    }

    private String dominantSymptom(Map<String, Integer> scores) {
        String dominant = null;
        int best = 0;
        for (String axis : AXIS_ORDER) {
            int score = scores.getOrDefault(axis, 0);
            if (score > best) {
                best = score;
                dominant = axis;
            }
        }
        return dominant;
    }

    private String writeScores(Map<String, Integer> scores) {
        return objectMapper.writeValueAsString(scores);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Integer> readScores(String json) {
        try {
            Map<String, Object> raw = objectMapper.readValue(json, Map.class);
            Map<String, Integer> scores = new LinkedHashMap<>();
            for (String axis : AXIS_ORDER) {
                Object value = raw.get(axis);
                if (value instanceof Number number) {
                    scores.put(axis, number.intValue());
                }
            }
            return scores;
        } catch (JacksonException exception) {
            return Map.of();
        }
    }
}
