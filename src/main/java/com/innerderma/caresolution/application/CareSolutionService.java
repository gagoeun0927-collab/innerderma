package com.innerderma.caresolution.application;

import com.innerderma.carecycle.domain.CareCycle;
import com.innerderma.carecycle.domain.CareCycleRepository;
import com.innerderma.caresolution.domain.*;
import com.innerderma.common.error.BusinessException;
import com.innerderma.common.error.ErrorCode;
import com.innerderma.procedure.domain.ProcedureRecord;
import com.innerderma.procedure.domain.ProcedureRecordRepository;
import com.innerderma.skinanalysis.application.SkinAgeAnalysisResult;
import com.innerderma.skindiagnosis.domain.WhsSkinDiagnosis;
import com.innerderma.skindiagnosis.domain.WhsSkinDiagnosisRepository;
import com.innerderma.user.domain.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.time.*;
import java.util.*;

@Service
@Transactional(readOnly = true)
public class CareSolutionService {
    private static final ZoneId MVP_ZONE = ZoneId.of("Asia/Seoul");
    private static final int PROCEDURE_GUIDE_DAYS = 14;

    private final CareSolutionRepository solutionRepository;
    private final CareCycleRepository cycleRepository;
    private final WhsSkinDiagnosisRepository diagnosisRepository;
    private final ProcedureRecordRepository procedureRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    @Autowired
    public CareSolutionService(CareSolutionRepository solutionRepository,
                               CareCycleRepository cycleRepository,
                               WhsSkinDiagnosisRepository diagnosisRepository,
                               ProcedureRecordRepository procedureRepository,
                               UserRepository userRepository, ObjectMapper objectMapper) {
        this(solutionRepository, cycleRepository, diagnosisRepository, procedureRepository,
                userRepository, objectMapper, Clock.system(MVP_ZONE));
    }

    CareSolutionService(CareSolutionRepository solutionRepository,
                        CareCycleRepository cycleRepository,
                        WhsSkinDiagnosisRepository diagnosisRepository,
                        ProcedureRecordRepository procedureRepository,
                        UserRepository userRepository, ObjectMapper objectMapper, Clock clock) {
        this.solutionRepository = solutionRepository;
        this.cycleRepository = cycleRepository;
        this.diagnosisRepository = diagnosisRepository;
        this.procedureRepository = procedureRepository;
        this.userRepository = userRepository;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    @Transactional
    public CareSolutionResult create(String userCode, Long careCycleId) {
        ensureUser(userCode);
        LocalDate today = LocalDate.now(clock);
        CareCycle cycle = careCycleId == null
                ? cycleRepository
                    .findFirstByUser_UserCodeAndOriginCaptureDateLessThanEqualOrderByOriginCaptureDateDescCreatedAtDesc(
                            userCode, today)
                    .orElseThrow(() -> new BusinessException(ErrorCode.CARE_CYCLE_NOT_FOUND))
                : cycleRepository.findByIdAndUser_UserCode(careCycleId, userCode)
                    .orElseThrow(() -> new BusinessException(ErrorCode.CARE_CYCLE_NOT_FOUND));
        if (solutionRepository.existsByCareCycle_Id(cycle.getId())) {
            throw new BusinessException(ErrorCode.CARE_SOLUTION_ALREADY_EXISTS);
        }

        LocalDate originDate = cycle.getOriginCaptureDate();
        WhsSkinDiagnosis diagnosis = diagnosisRepository
                .findFirstByUser_UserCodeAndDiagnosedDateLessThanEqualOrderByDiagnosedDateDesc(
                        userCode, originDate).orElse(null);
        ProcedureRecord procedure = procedureRepository
                .findFirstByUser_UserCodeAndProcedureDateLessThanEqualOrderByProcedureDateDesc(userCode, originDate)
                .filter(record -> !record.getProcedureDate().isBefore(originDate.minusDays(PROCEDURE_GUIDE_DAYS)))
                .orElse(null);
        SkinAgeAnalysisResult analysis = readAnalysis(cycle);
        String concern = primaryConcern(analysis);
        boolean attention = cycle.getSelfCheck() != null && cycle.getSelfCheck().requiresSafetyAttention();
        CareSeason season = CareSeason.from(originDate);

        List<String> evening = eveningSteps(attention, concern, season, procedure);
        List<String> morning = morningSteps(attention, season, procedure);
        SafetyLevel safetyLevel = attention ? SafetyLevel.ATTENTION : SafetyLevel.NORMAL;
        String safetyMessage = safetyMessage(attention, procedure);
        String headline = attention
                ? "오늘은 피부 자극을 줄이고 안전 신호를 먼저 살펴보세요."
                : headline(concern, season);

        CareSolution solution = new CareSolution(cycle, diagnosis, procedure, season, safetyLevel,
                headline, write(evening), write(morning), safetyMessage, concern, LocalDateTime.now(clock));
        return new CareSolutionResult(solutionRepository.save(solution), evening, morning, originDate);
    }

    public CareSolutionResult getDaily(String userCode, LocalDate date) {
        ensureUser(userCode);
        LocalDate servedDate = date == null ? LocalDate.now(clock) : date;
        CareSolution solution = solutionRepository
                .findFirstByCareCycle_User_UserCodeAndCareCycle_OriginCaptureDateLessThanEqualOrderByCareCycle_OriginCaptureDateDescGeneratedAtDesc(
                        userCode, servedDate)
                .orElseThrow(() -> new BusinessException(ErrorCode.CARE_SOLUTION_NOT_FOUND));
        return new CareSolutionResult(solution, readSteps(solution.getEveningStepsJson()),
                readSteps(solution.getMorningStepsJson()), servedDate);
    }

    private void ensureUser(String userCode) {
        if (!userRepository.existsByUserCode(userCode)) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
    }

    private List<String> eveningSteps(boolean attention, String concern, CareSeason season,
                                      ProcedureRecord procedure) {
        List<String> steps = new ArrayList<>();
        steps.add("미지근한 물과 순한 세안제로 피부를 문지르지 않고 세안하세요.");
        if (attention) {
            steps.add("각질 제거제, 레티놀, 고함량 비타민C 등 자극 가능 제품은 오늘 사용하지 마세요.");
            steps.add("향이 강하지 않은 보습제를 얇게 바르고 피부 반응을 확인하세요.");
        } else {
            steps.add(concernStep(concern));
            steps.add(seasonEveningStep(season));
        }
        if (procedure != null) {
            steps.add("시술기관 안내를 우선해 주세요: " + procedure.getCareGuide());
        }
        return List.copyOf(steps);
    }

    private List<String> morningSteps(boolean attention, CareSeason season, ProcedureRecord procedure) {
        List<String> steps = new ArrayList<>();
        steps.add("아침에는 피부 상태에 따라 물 세안 또는 순한 세안제를 사용하세요.");
        steps.add(attention ? "최소한의 순한 보습만 적용하고 새로운 제품은 시작하지 마세요."
                : "가벼운 보습제로 피부 장벽을 보호하세요.");
        steps.add("외출 전 자외선 차단제를 충분히 바르고 필요할 때 덧바르세요.");
        if (season == CareSeason.SUMMER) {
            steps.add("땀과 피지는 문지르지 말고 부드럽게 눌러 닦아 주세요.");
        } else if (season == CareSeason.WINTER) {
            steps.add("건조한 실내에서는 보습이 끊기지 않도록 피부 당김을 확인하세요.");
        }
        if (procedure != null) {
            steps.add("시술 후 별도 주의사항이 있다면 기관 안내를 우선하세요.");
        }
        return List.copyOf(steps);
    }

    private String primaryConcern(SkinAgeAnalysisResult analysis) {
        return analysis.aggregateMetrics().concernAverages().entrySet().stream()
                .min(Map.Entry.comparingByValue()).map(Map.Entry::getKey).orElse("overall");
    }

    private String concernStep(String concern) {
        return switch (concern) {
            case "redness" -> "홍조가 두드러진 부위에는 마찰을 줄이고 진정 중심의 보습을 적용하세요.";
            case "pore_texture" -> "피부결 관리를 위해 강한 스크럽 대신 충분한 보습을 적용하세요.";
            case "pigmentation" -> "색소 관리 중에는 자극적인 집중 관리보다 보습과 자외선 차단을 우선하세요.";
            case "wrinkle" -> "건조로 인한 잔주름이 심해지지 않도록 보습제를 고르게 적용하세요.";
            default -> "피부 장벽을 보호할 수 있도록 순한 보습 중심으로 관리하세요.";
        };
    }

    private String seasonEveningStep(CareSeason season) {
        return switch (season) {
            case SPRING -> "외부 자극이 많은 계절이므로 새로운 활성 성분은 천천히 적용하세요.";
            case SUMMER -> "무거운 제품을 여러 겹 바르기보다 가벼운 보습을 얇게 적용하세요.";
            case AUTUMN -> "건조 전환기에 대비해 평소보다 보습 단계를 꼼꼼히 적용하세요.";
            case WINTER -> "피부 장벽 보호를 위해 보습제를 충분히 적용하세요.";
        };
    }

    private String headline(String concern, CareSeason season) {
        return "오늘은 " + concernLabel(concern) + " 관리와 " + seasonLabel(season) + " 케어에 집중하세요.";
    }

    private String concernLabel(String concern) {
        return switch (concern) {
            case "redness" -> "홍조"; case "pore_texture" -> "모공·피부결";
            case "pigmentation" -> "색소"; case "wrinkle" -> "주름"; default -> "피부 장벽";
        };
    }

    private String seasonLabel(CareSeason season) {
        return switch (season) {
            case SPRING -> "외부 자극 대비"; case SUMMER -> "가벼운 보습";
            case AUTUMN -> "건조 전환기"; case WINTER -> "집중 보습";
        };
    }

    private String safetyMessage(boolean attention, ProcedureRecord procedure) {
        if (attention) {
            return "통증, 열감, 붓기 또는 심한 불편감이 지속되거나 악화되면 일반 홈케어를 중단하고 시술기관 또는 의료진에게 문의하세요.";
        }
        return procedure == null ? null : "시술 후에는 일반 안내보다 시술기관에서 받은 주의사항을 우선하세요.";
    }

    private SkinAgeAnalysisResult readAnalysis(CareCycle cycle) {
        try {
            return objectMapper.readValue(cycle.getSkinAnalysis().getRawResult(), SkinAgeAnalysisResult.class);
        } catch (RuntimeException exception) {
            throw new BusinessException(ErrorCode.SKINAGE_INVALID_RESPONSE);
        }
    }

    private String write(List<String> steps) {
        try { return objectMapper.writeValueAsString(steps); }
        catch (JacksonException exception) { throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR); }
    }

    private List<String> readSteps(String json) {
        try { return objectMapper.readValue(json, new TypeReference<List<String>>() {}); }
        catch (JacksonException exception) { throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR); }
    }
}
