package com.innerderma.carecycle.application;

import com.innerderma.carecycle.domain.CareCycle;
import com.innerderma.carecycle.domain.CareCycleRepository;
import com.innerderma.common.error.BusinessException;
import com.innerderma.common.error.ErrorCode;
import com.innerderma.selfcheck.domain.SelfCheck;
import com.innerderma.selfcheck.domain.SelfCheckRepository;
import com.innerderma.skinanalysis.domain.SkinAnalysis;
import com.innerderma.skinanalysis.domain.SkinAnalysisRepository;
import com.innerderma.user.domain.User;
import com.innerderma.user.domain.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;

@Service
@Transactional(readOnly = true)
public class CareCycleService {
    private static final ZoneId MVP_ZONE = ZoneId.of("Asia/Seoul");

    private final CareCycleRepository careCycleRepository;
    private final SkinAnalysisRepository skinAnalysisRepository;
    private final SelfCheckRepository selfCheckRepository;
    private final UserRepository userRepository;
    private final Clock clock;

    @Autowired
    public CareCycleService(CareCycleRepository careCycleRepository,
                            SkinAnalysisRepository skinAnalysisRepository,
                            SelfCheckRepository selfCheckRepository,
                            UserRepository userRepository) {
        this(careCycleRepository, skinAnalysisRepository, selfCheckRepository,
                userRepository, Clock.system(MVP_ZONE));
    }

    CareCycleService(CareCycleRepository careCycleRepository,
                     SkinAnalysisRepository skinAnalysisRepository,
                     SelfCheckRepository selfCheckRepository,
                     UserRepository userRepository, Clock clock) {
        this.careCycleRepository = careCycleRepository;
        this.skinAnalysisRepository = skinAnalysisRepository;
        this.selfCheckRepository = selfCheckRepository;
        this.userRepository = userRepository;
        this.clock = clock;
    }

    @Transactional
    public CareCycleResult create(String userCode) {
        User user = userRepository.findByUserCode(userCode)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        SkinAnalysis analysis = skinAnalysisRepository
                .findFirstBySkinCapture_User_UserCodeOrderByAnalyzedAtDesc(userCode)
                .orElseThrow(() -> new BusinessException(ErrorCode.SKIN_ANALYSIS_NOT_FOUND));
        if (careCycleRepository.existsBySkinAnalysis_Id(analysis.getId())) {
            throw new BusinessException(ErrorCode.CARE_CYCLE_ALREADY_EXISTS);
        }
        SelfCheck selfCheck = selfCheckRepository.findFirstByUser_UserCodeOrderByCheckedAtDesc(userCode)
                .orElse(null);
        CareCycle careCycle = new CareCycle(user, analysis, selfCheck,
                analysis.getSkinCapture().getCapturedDate(), LocalDateTime.now(clock));
        return new CareCycleResult(careCycleRepository.save(careCycle),
                careCycle.getOriginCaptureDate());
    }

    public CareCycleResult getDaily(String userCode, LocalDate date) {
        if (!userRepository.existsByUserCode(userCode)) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        LocalDate servedDate = date == null ? LocalDate.now(clock) : date;
        CareCycle careCycle = careCycleRepository
                .findFirstByUser_UserCodeAndOriginCaptureDateLessThanEqualOrderByOriginCaptureDateDescCreatedAtDesc(
                        userCode, servedDate)
                .orElseThrow(() -> new BusinessException(ErrorCode.CARE_CYCLE_NOT_FOUND));
        return new CareCycleResult(careCycle, servedDate);
    }
}
