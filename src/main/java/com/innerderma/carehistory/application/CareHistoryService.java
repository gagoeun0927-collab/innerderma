package com.innerderma.carehistory.application;

import com.innerderma.caresolution.domain.CareSolutionRepository;
import com.innerderma.common.error.BusinessException;
import com.innerderma.common.error.ErrorCode;
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
    private final CareSolutionRepository careSolutionRepository;

    public CareHistoryService(UserRepository userRepository, CareSolutionRepository careSolutionRepository) {
        this.userRepository = userRepository;
        this.careSolutionRepository = careSolutionRepository;
    }

    public CareHistoryResult getHistory(String userCode, LocalDate from, LocalDate to) {
        if (!userRepository.existsByUserCode(userCode)) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }

        LocalDate resolvedTo = to == null ? LocalDate.now(SEOUL) : to;
        LocalDate resolvedFrom = from == null ? resolvedTo.minusDays(29) : from;
        validateRange(resolvedFrom, resolvedTo);

        var items = careSolutionRepository
                .findByCareCycle_User_UserCodeAndCareCycle_OriginCaptureDateBetweenOrderByCareCycle_OriginCaptureDateDesc(
                        userCode, resolvedFrom, resolvedTo)
                .stream()
                .map(solution -> {
                    var cycle = solution.getCareCycle();
                    var analysis = cycle.getSkinAnalysis();
                    return new CareHistoryItem(cycle.getOriginCaptureDate(), analysis.getSkinCapture().getId(),
                            analysis.getId(), cycle.getId(), solution.getId(), solution.getSeason(),
                            solution.getSafetyLevel(), solution.getHeadline(), solution.getPrimaryConcern(),
                            cycle.getSelfCheck() != null, solution.getGeneratedAt());
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
