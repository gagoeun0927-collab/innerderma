package com.innerderma.skinstate.trend;

import com.innerderma.common.error.BusinessException;
import com.innerderma.common.error.ErrorCode;
import com.innerderma.skinstate.application.SkinStateSnapshotService;
import com.innerderma.skinstate.domain.SkinStateSnapshot;
import com.innerderma.skinstate.domain.SkinStateSnapshotRepository;
import com.innerderma.user.domain.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Trend Engine v1 (R005). 같은 scoringVersion 의 최신 스냅샷과 직전 스냅샷 2개만 비교한다.
 *
 * <p>증상별 delta = 오늘 점수 - 이전 점수: {@code >0 → WORSENING}, {@code <0 → IMPROVING}, {@code 0 → STABLE}.
 * 전체 trend 는 8개 증상 점수 합계의 변화 방향으로 판정한다. 비교 가능한 스냅샷이 2개 미만이거나
 * scoringVersion 이 다르면 UNKNOWN 이다. 임계값/deadband 는 두지 않는다(부호만 사용). 임상 진단이 아니라
 * 스냅샷 간 결정적 변화 비교값이다. 데이터가 충분히 쌓이면 별도 version 에서 N일 window 방식으로 확장한다.
 */
@Service
@Transactional(readOnly = true)
public class SkinStateTrendService {

    private final SkinStateSnapshotRepository snapshotRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    public SkinStateTrendService(SkinStateSnapshotRepository snapshotRepository,
                                 UserRepository userRepository, ObjectMapper objectMapper) {
        this.snapshotRepository = snapshotRepository;
        this.userRepository = userRepository;
        this.objectMapper = objectMapper;
    }

    public TrendResult evaluateLatest(String userCode) {
        if (!userRepository.existsByUserCode(userCode)) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        List<SkinStateSnapshot> recent = snapshotRepository
                .findTop2ByUser_UserCodeOrderBySnapshotDateDesc(userCode);

        if (recent.size() < 2) {
            String version = recent.isEmpty() ? null : recent.get(0).getScoringVersion();
            var latestDate = recent.isEmpty() ? null : recent.get(0).getSnapshotDate();
            return TrendResult.unknown(version, latestDate, null);
        }

        SkinStateSnapshot latest = recent.get(0);
        SkinStateSnapshot previous = recent.get(1);
        if (!latest.getScoringVersion().equals(previous.getScoringVersion())) {
            return TrendResult.unknown(latest.getScoringVersion(),
                    latest.getSnapshotDate(), previous.getSnapshotDate());
        }

        Map<String, Integer> latestScores = readScores(latest.getSymptomScoresJson());
        Map<String, Integer> previousScores = readScores(previous.getSymptomScoresJson());

        Map<String, SkinStateTrend> symptomTrends = new LinkedHashMap<>();
        int latestSum = 0;
        int previousSum = 0;
        for (String axis : SkinStateSnapshotService.AXIS_ORDER) {
            int today = latestScores.getOrDefault(axis, 0);
            int before = previousScores.getOrDefault(axis, 0);
            latestSum += today;
            previousSum += before;
            symptomTrends.put(axis, classify(today - before));
        }

        return new TrendResult(classify(latestSum - previousSum), symptomTrends,
                latest.getScoringVersion(), latest.getSnapshotDate(), previous.getSnapshotDate());
    }

    private SkinStateTrend classify(int delta) {
        if (delta > 0) {
            return SkinStateTrend.WORSENING;
        }
        if (delta < 0) {
            return SkinStateTrend.IMPROVING;
        }
        return SkinStateTrend.STABLE;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Integer> readScores(String json) {
        Map<String, Integer> scores = new LinkedHashMap<>();
        try {
            Map<String, Object> raw = objectMapper.readValue(json, Map.class);
            for (String axis : SkinStateSnapshotService.AXIS_ORDER) {
                Object value = raw.get(axis);
                scores.put(axis, value instanceof Number number ? number.intValue() : 0);
            }
        } catch (JacksonException exception) {
            for (String axis : SkinStateSnapshotService.AXIS_ORDER) {
                scores.put(axis, 0);
            }
        }
        return scores;
    }
}
