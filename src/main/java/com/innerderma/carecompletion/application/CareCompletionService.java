package com.innerderma.carecompletion.application;

import com.innerderma.carecompletion.domain.*;
import com.innerderma.carehistory.application.CarePhase;
import com.innerderma.caresolution.application.CareSolutionService;
import com.innerderma.common.error.*;
import com.innerderma.user.domain.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.util.List;
import java.time.temporal.ChronoUnit;

@Service
public class CareCompletionService {
    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");
    private final UserRepository userRepository;
    private final CareCompletionRepository repository;
    private final CareSolutionService careSolutionService;

    public CareCompletionService(UserRepository userRepository, CareCompletionRepository repository,
                                 CareSolutionService careSolutionService) {
        this.userRepository = userRepository;
        this.repository = repository;
        this.careSolutionService = careSolutionService;
    }

    @Transactional
    public CareCompletion save(String userCode, LocalDate servedDate, CarePhase phase, boolean completed) {
        var user = userRepository.findByUserCode(userCode)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        LocalDate date = servedDate == null ? LocalDate.now(SEOUL) : servedDate;
        LocalDate solutionLookupDate = phase == CarePhase.MORNING ? date.minusDays(1) : date;
        var solution = careSolutionService.getDaily(userCode, solutionLookupDate).solution();
        LocalDateTime now = LocalDateTime.now(SEOUL);
        var completion = repository.findByUser_UserCodeAndServedDateAndPhase(userCode, date, phase)
                .map(existing -> {
                    existing.update(solution, completed, now);
                    return existing;
                })
                .orElseGet(() -> new CareCompletion(user, solution, date, phase, completed, now));
        return repository.save(completion);
    }

    @Transactional(readOnly = true)
    public List<CareCompletion> getDaily(String userCode, LocalDate servedDate) {
        if (!userRepository.existsByUserCode(userCode)) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        LocalDate date = servedDate == null ? LocalDate.now(SEOUL) : servedDate;
        return repository.findByUser_UserCodeAndServedDateOrderByPhaseAsc(userCode, date);
    }

    @Transactional(readOnly = true)
    public CareCompletionHistoryResult getHistory(String userCode, LocalDate from, LocalDate to) {
        if (!userRepository.existsByUserCode(userCode)) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        LocalDate resolvedTo = to == null ? LocalDate.now(SEOUL) : to;
        LocalDate resolvedFrom = from == null ? resolvedTo.minusDays(29) : from;
        if (resolvedFrom.isAfter(resolvedTo)
                || ChronoUnit.DAYS.between(resolvedFrom, resolvedTo) + 1 > 31) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }
        return new CareCompletionHistoryResult(resolvedFrom, resolvedTo,
                repository.findByUser_UserCodeAndServedDateBetweenOrderByServedDateDescPhaseAsc(
                        userCode, resolvedFrom, resolvedTo));
    }
}
