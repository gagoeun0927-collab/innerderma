package com.innerderma.carehistory.application;

import com.innerderma.carecycle.domain.CareCycleRepository;
import com.innerderma.caresolution.domain.CareSolutionRepository;
import com.innerderma.common.error.BusinessException;
import com.innerderma.common.error.ErrorCode;
import com.innerderma.skinanalysis.domain.SkinAnalysisRepository;
import com.innerderma.skincapture.domain.SkinCaptureRepository;
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
                .findByUser_UserCodeAndCapturedDateBetweenOrderByCapturedDateDescCapturedAtDesc(
                        userCode, resolvedFrom, resolvedTo)
                .stream()
                .map(capture -> {
                    var analysis = analysisRepository.findBySkinCapture_Id(capture.getId()).orElse(null);
                    var cycle = analysis == null ? null : cycleRepository.findBySkinAnalysis_Id(analysis.getId()).orElse(null);
                    var solution = cycle == null ? null : careSolutionRepository.findByCareCycle_Id(cycle.getId()).orElse(null);
                    CareProgressStatus status = solution != null ? CareProgressStatus.SOLUTION_READY
                            : cycle != null ? CareProgressStatus.CYCLE_CREATED
                            : analysis != null ? CareProgressStatus.ANALYZED : CareProgressStatus.CAPTURED;
                    return new CareHistoryItem(capture.getCapturedDate(), capture.getId(),
                            analysis == null ? null : analysis.getId(), cycle == null ? null : cycle.getId(),
                            solution == null ? null : solution.getId(), status,
                            solution == null ? null : solution.getSeason(),
                            solution == null ? null : solution.getSafetyLevel(),
                            solution == null ? null : solution.getHeadline(),
                            solution == null ? null : solution.getPrimaryConcern(),
                            cycle != null && cycle.getSelfCheck() != null,
                            solution == null ? null : solution.getGeneratedAt());
                })
                .toList();
        return new CareHistoryResult(resolvedFrom, resolvedTo, items);
    }

    private void validateRange(LocalDate from, LocalDate to) {
        if (from.isAfter(to) || ChronoUnit.DAYS.between(from, to) + 1 > MAX_RANGE_DAYS) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }
    }
}
