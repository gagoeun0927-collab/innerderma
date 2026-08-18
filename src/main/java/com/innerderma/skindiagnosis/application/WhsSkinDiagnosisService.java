package com.innerderma.skindiagnosis.application;

import com.innerderma.common.error.BusinessException;
import com.innerderma.common.error.ErrorCode;
import com.innerderma.skindiagnosis.domain.WhsSkinDiagnosis;
import com.innerderma.skindiagnosis.domain.WhsSkinDiagnosisRepository;
import com.innerderma.user.domain.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;

@Service
@Transactional(readOnly = true)
public class WhsSkinDiagnosisService {

    private static final ZoneId MVP_ZONE = ZoneId.of("Asia/Seoul");
    private static final long MAX_RANGE_DAYS = 31;

    private final WhsSkinDiagnosisRepository diagnosisRepository;
    private final UserRepository userRepository;
    private final Clock clock;

    @Autowired
    public WhsSkinDiagnosisService(WhsSkinDiagnosisRepository diagnosisRepository,
                                   UserRepository userRepository) {
        this(diagnosisRepository, userRepository, Clock.system(MVP_ZONE));
    }

    WhsSkinDiagnosisService(WhsSkinDiagnosisRepository diagnosisRepository,
                            UserRepository userRepository, Clock clock) {
        this.diagnosisRepository = diagnosisRepository;
        this.userRepository = userRepository;
        this.clock = clock;
    }

    public WhsSkinDiagnosis getLatestDiagnosis(String userCode) {
        return diagnosisRepository.findTopByUser_UserCodeOrderByDiagnosedDateDesc(userCode)
                .orElseThrow(() -> new BusinessException(ErrorCode.SKIN_DIAGNOSIS_NOT_FOUND));
    }

    public WhsSkinDiagnosisHistoryResult getHistory(String userCode, LocalDate from, LocalDate to) {
        if (!userRepository.existsByUserCode(userCode)) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        LocalDate resolvedTo = to == null ? LocalDate.now(clock) : to;
        LocalDate resolvedFrom = from == null ? resolvedTo.minusDays(29) : from;
        if (resolvedFrom.isAfter(resolvedTo)
                || ChronoUnit.DAYS.between(resolvedFrom, resolvedTo) + 1 > MAX_RANGE_DAYS) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }
        var items = diagnosisRepository.findByUser_UserCodeAndDiagnosedDateBetweenOrderByDiagnosedDateDesc(
                userCode, resolvedFrom, resolvedTo);
        return new WhsSkinDiagnosisHistoryResult(resolvedFrom, resolvedTo, items);
    }
}
