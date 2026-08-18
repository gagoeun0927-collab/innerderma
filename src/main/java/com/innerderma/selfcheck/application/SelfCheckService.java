package com.innerderma.selfcheck.application;

import com.innerderma.common.error.BusinessException;
import com.innerderma.common.error.ErrorCode;
import com.innerderma.selfcheck.domain.SelfCheck;
import com.innerderma.selfcheck.domain.SelfCheckRepository;
import com.innerderma.user.domain.User;
import com.innerderma.user.domain.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;

@Service
@Transactional(readOnly = true)
public class SelfCheckService {

    private static final ZoneId MVP_ZONE = ZoneId.of("Asia/Seoul");
    private static final long MAX_RANGE_DAYS = 31;

    private final SelfCheckRepository selfCheckRepository;
    private final UserRepository userRepository;
    private final Clock clock;

    @Autowired
    public SelfCheckService(SelfCheckRepository selfCheckRepository, UserRepository userRepository) {
        this(selfCheckRepository, userRepository, Clock.system(MVP_ZONE));
    }

    SelfCheckService(SelfCheckRepository selfCheckRepository, UserRepository userRepository, Clock clock) {
        this.selfCheckRepository = selfCheckRepository;
        this.userRepository = userRepository;
        this.clock = clock;
    }

    @Transactional
    public SelfCheck create(String userCode, SelfCheckCommand command) {
        User user = userRepository.findByUserCode(userCode)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        SelfCheck selfCheck = new SelfCheck(
                user,
                LocalDateTime.now(clock),
                command.pain(),
                command.heatSensation(),
                command.tightness(),
                command.dryness(),
                command.itching(),
                command.swelling(),
                command.peeling(),
                command.breakout(),
                command.note()
        );
        return selfCheckRepository.save(selfCheck);
    }

    public SelfCheck getLatest(String userCode) {
        if (!userRepository.existsByUserCode(userCode)) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        return selfCheckRepository.findFirstByUser_UserCodeOrderByCheckedAtDesc(userCode)
                .orElseThrow(() -> new BusinessException(ErrorCode.SELF_CHECK_NOT_FOUND));
    }

    public SelfCheckHistoryResult getHistory(String userCode, LocalDate from, LocalDate to) {
        if (!userRepository.existsByUserCode(userCode)) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        LocalDate resolvedTo = to == null ? LocalDate.now(clock) : to;
        LocalDate resolvedFrom = from == null ? resolvedTo.minusDays(29) : from;
        if (resolvedFrom.isAfter(resolvedTo)
                || ChronoUnit.DAYS.between(resolvedFrom, resolvedTo) + 1 > MAX_RANGE_DAYS) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }
        var items = selfCheckRepository.findByUser_UserCodeAndCheckedAtBetweenOrderByCheckedAtDesc(
                userCode, resolvedFrom.atStartOfDay(), resolvedTo.atTime(LocalTime.MAX));
        return new SelfCheckHistoryResult(resolvedFrom, resolvedTo, items);
    }
}
