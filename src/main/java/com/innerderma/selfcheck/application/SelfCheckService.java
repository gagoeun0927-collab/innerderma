package com.innerderma.selfcheck.application;

import com.innerderma.airule.cache.SolutionCache;
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
import java.time.LocalDateTime;
import java.time.ZoneId;

@Service
@Transactional(readOnly = true)
public class SelfCheckService {

    private static final ZoneId MVP_ZONE = ZoneId.of("Asia/Seoul");

    private final SelfCheckRepository selfCheckRepository;
    private final UserRepository userRepository;
    private final SolutionCache solutionCache;
    private final Clock clock;

    @Autowired
    public SelfCheckService(SelfCheckRepository selfCheckRepository, UserRepository userRepository, SolutionCache solutionCache) {
        this(selfCheckRepository, userRepository, solutionCache, Clock.system(MVP_ZONE));
    }

    SelfCheckService(SelfCheckRepository selfCheckRepository, UserRepository userRepository, SolutionCache solutionCache, Clock clock) {
        this.selfCheckRepository = selfCheckRepository;
        this.userRepository = userRepository;
        this.solutionCache = solutionCache;
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
                command.oozing(),
                command.bleeding(),
                command.barrierDamage(),
                command.note()
        );
        SelfCheck saved = selfCheckRepository.save(selfCheck);
        solutionCache.invalidate(userCode);
        return saved;
    }

    public SelfCheck getLatest(String userCode) {
        if (!userRepository.existsByUserCode(userCode)) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        return selfCheckRepository.findFirstByUser_UserCodeOrderByCheckedAtDesc(userCode)
                .orElseThrow(() -> new BusinessException(ErrorCode.SELF_CHECK_NOT_FOUND));
    }
}
