package com.innerderma.carehistory.application;

import com.innerderma.carecycle.domain.CareCycleRepository;
import com.innerderma.caresolution.domain.CareSolutionRepository;
import com.innerderma.common.error.BusinessException;
import com.innerderma.common.error.ErrorCode;
import com.innerderma.skinanalysis.domain.SkinAnalysisRepository;
import com.innerderma.skincapture.domain.SkinCaptureRepository;
import com.innerderma.skincapture.domain.SkinCaptureQualityStatus;
import com.innerderma.user.domain.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;

@Service
@Transactional(readOnly = true)
public class CareHistoryService {
    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");
    private static final long MAX_RANGE_DAYS = 31;

    private final UserRepository userRepository;
    private final SkinCaptureRepository captureRepository;
    private final SkinAnalysisRepository analysisRepository;
    private final CareCycleRepository cycleRepository;
    private final CareSolutionRepository careSolutionRepository;

    public CareHistoryService(UserRepository userRepository, SkinCaptureRepository captureRepository,
                              SkinAnalysisRepository analysisRepository, CareCycleRepository cycleRepository,
                              CareSolutionRepository careSolutionRepository) {
        this.userRepository = userRepository;
        this.captureRepository = captureRepository;
        this.analysisRepository = analysisRepository;
        this.cycleRepository = cycleRepository;
        this.careSolutionRepository = careSolutionRepository;
    }

    public CareHistoryResult getHistory(String userCode, LocalDate from, LocalDate to) {
        if (!userRepository.existsByUserCode(userCode)) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }

        LocalDate resolvedTo = to == null ? LocalDate.now(SEOUL) : to;
        LocalDate resolvedFrom = from == null ? resolvedTo.minusDays(29) : from;
        validateRange(resolvedFrom, resolvedTo);

        var items = captureRepository
                .findByUser_UserCodeAndCapturedDateBetweenAndQualityStatusOrderByCapturedDateDescCapturedAtDesc(
                        userCode, resolvedFrom, resolvedTo, SkinCaptureQualityStatus.VALID)
                .stream()
                .map(this::toItem)
                .toList();
        return new CareHistoryResult(resolvedFrom, resolvedTo, items);
    }

    public DailyCareHistoryResult getDailyDetail(String userCode, LocalDate date) {
        if (date == null) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }
        if (!userRepository.existsByUserCode(userCode)) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }

        var items = new java.util.ArrayList<DailyCareHistoryItem>(2);
        // 기상 후 단계는 당일 새 촬영 전에 확정되어 있으므로 전날까지의 최신 솔루션을 사용한다.
        careSolutionRepository
                .findFirstByCareCycle_User_UserCodeAndCareCycle_OriginCaptureDateLessThanEqualOrderByCareCycle_OriginCaptureDateDescGeneratedAtDesc(
                        userCode, date.minusDays(1))
                .ifPresent(solution -> items.add(new DailyCareHistoryItem(
                        CarePhase.MORNING, true, toItem(solution.getCareCycle().getSkinAnalysis().getSkinCapture()))));

        // 귀가 후 단계는 당일 새 솔루션이 있으면 교체하고, 없으면 가장 최근 솔루션을 승계한다.
        careSolutionRepository
                .findFirstByCareCycle_User_UserCodeAndCareCycle_OriginCaptureDateLessThanEqualOrderByCareCycle_OriginCaptureDateDescGeneratedAtDesc(
                        userCode, date)
                .ifPresent(solution -> items.add(new DailyCareHistoryItem(
                        CarePhase.EVENING,
                        !date.equals(solution.getCareCycle().getOriginCaptureDate()),
                        toItem(solution.getCareCycle().getSkinAnalysis().getSkinCapture()))));

        // 아직 새 솔루션이 완성되지 않았고 과거 솔루션도 없다면 당일 처리 상태는 계속 보여 준다.
        if (items.stream().noneMatch(item -> item.phase() == CarePhase.EVENING)) {
            getHistory(userCode, date, date).items().stream().findFirst()
                    .ifPresent(history -> items.add(new DailyCareHistoryItem(CarePhase.EVENING, false, history)));
        }
        if (items.isEmpty()) {
            throw new BusinessException(ErrorCode.CARE_HISTORY_NOT_FOUND);
        }
        items.sort(java.util.Comparator.comparing(DailyCareHistoryItem::phase));
        return new DailyCareHistoryResult(date, java.util.List.copyOf(items));
    }

    private CareHistoryItem toItem(com.innerderma.skincapture.domain.SkinCapture capture) {
        var analysis = analysisRepository.findBySkinCapture_Id(capture.getId()).orElse(null);
        var cycle = analysis == null ? null : cycleRepository.findBySkinAnalysis_Id(analysis.getId()).orElse(null);
        var solution = cycle == null ? null : careSolutionRepository.findByCareCycle_Id(cycle.getId()).orElse(null);
        CareProgressStatus status = solution != null ? CareProgressStatus.SOLUTION_READY
                : cycle != null ? CareProgressStatus.CYCLE_CREATED
                : analysis != null ? CareProgressStatus.ANALYZED : CareProgressStatus.CAPTURED;
        return new CareHistoryItem(capture.getCapturedDate(),
                cycle == null ? capture.getCapturedDate() : cycle.getEveningCareDate(),
                cycle == null ? null : cycle.getMorningCareDate(), capture.getId(),
                analysis == null ? null : analysis.getId(), cycle == null ? null : cycle.getId(),
                solution == null ? null : solution.getId(), status,
                solution == null ? null : solution.getSeason(),
                solution == null ? null : solution.getSafetyLevel(),
                solution == null ? null : solution.getHeadline(),
                solution == null ? null : solution.getPrimaryConcern(),
                cycle != null && cycle.getSelfCheck() != null,
                solution == null ? null : solution.getGeneratedAt());
    }

    private void validateRange(LocalDate from, LocalDate to) {
        if (from.isAfter(to) || ChronoUnit.DAYS.between(from, to) + 1 > MAX_RANGE_DAYS) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }
    }
}
