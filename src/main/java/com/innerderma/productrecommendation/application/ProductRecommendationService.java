package com.innerderma.productrecommendation.application;

import com.innerderma.caresolution.domain.CareSolution;
import com.innerderma.caresolution.domain.CareSolutionRepository;
import com.innerderma.caresolution.domain.SafetyLevel;
import com.innerderma.common.error.BusinessException;
import com.innerderma.common.error.ErrorCode;
import com.innerderma.product.domain.*;
import com.innerderma.user.domain.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.util.*;

@Service
@Transactional(readOnly = true)
public class ProductRecommendationService {
    private static final ZoneId MVP_ZONE = ZoneId.of("Asia/Seoul");
    private final CareSolutionRepository solutionRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final Clock clock;

    @Autowired
    public ProductRecommendationService(CareSolutionRepository solutionRepository,
                                        ProductRepository productRepository,
                                        UserRepository userRepository) {
        this(solutionRepository, productRepository, userRepository, Clock.system(MVP_ZONE));
    }

    ProductRecommendationService(CareSolutionRepository solutionRepository,
                                 ProductRepository productRepository,
                                 UserRepository userRepository, Clock clock) {
        this.solutionRepository = solutionRepository;
        this.productRepository = productRepository;
        this.userRepository = userRepository;
        this.clock = clock;
    }

    public ProductRecommendationResult getDaily(String userCode, LocalDate date) {
        if (!userRepository.existsByUserCode(userCode)) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        LocalDate servedDate = date == null ? LocalDate.now(clock) : date;
        CareSolution solution = solutionRepository
                .findFirstByCareCycle_User_UserCodeAndCareCycle_OriginCaptureDateLessThanEqualOrderByCareCycle_OriginCaptureDateDescGeneratedAtDesc(
                        userCode, servedDate)
                .orElseThrow(() -> new BusinessException(ErrorCode.CARE_SOLUTION_NOT_FOUND));

        ProductConcern concern = ProductConcern.fromAnalysisConcern(solution.getPrimaryConcern());
        boolean attention = solution.getSafetyLevel() == SafetyLevel.ATTENTION;
        List<Product> products = productRepository.findAllByActiveTrueOrderByDisplayPriorityAscProductCodeAsc();
        List<ProductRecommendationItem> items = select(products, concern, attention);
        String notice = attention
                ? "현재 안전 신호가 있어 최소 관리 제품 유형만 표시합니다. 증상이 지속되거나 악화되면 제품 사용보다 시술기관 또는 의료진 문의를 우선하세요."
                : "제품 정보는 일반적인 홈케어 참고용이며 의료 진단이나 개인별 적합성 판단을 대신하지 않습니다.";
        LocalDate originDate = solution.getCareCycle().getOriginCaptureDate();
        return new ProductRecommendationResult(originDate, servedDate,
                servedDate.isAfter(originDate), solution.getSafetyLevel(), items, notice);
    }

    private List<ProductRecommendationItem> select(List<Product> products,
                                                   ProductConcern concern, boolean attention) {
        EnumMap<ProductCategory, Product> selected = new EnumMap<>(ProductCategory.class);
        for (Product product : products) {
            if (attention && (!product.isSafetyAttentionCompatible()
                    || product.getCategory() == ProductCategory.TARGETED_CARE)) {
                continue;
            }
            if (product.getCategory() == ProductCategory.TARGETED_CARE
                    && product.getTargetConcern() != concern) {
                continue;
            }
            if (product.getCategory() != ProductCategory.TARGETED_CARE
                    && product.getTargetConcern() != ProductConcern.GENERAL
                    && product.getTargetConcern() != concern) {
                continue;
            }
            selected.putIfAbsent(product.getCategory(), product);
        }

        List<ProductRecommendationItem> result = new ArrayList<>();
        add(result, selected.get(ProductCategory.CLEANSER), "EVENING",
                "저녁 세안 단계에서 피부 마찰과 자극을 줄이기 위한 기본 제품 유형입니다.");
        add(result, selected.get(ProductCategory.MOISTURIZER), "EVENING_AND_MORNING",
                attention ? "안전 신호가 있어 활성 성분보다 기본 보습을 우선합니다."
                        : "저녁과 아침 피부 장벽 보호를 위한 기본 보습 단계입니다.");
        add(result, selected.get(ProductCategory.SUNSCREEN), "MORNING",
                "아침 자외선 노출로 인한 추가 자극을 줄이기 위한 기본 단계입니다.");
        if (!attention) {
            add(result, selected.get(ProductCategory.TARGETED_CARE), "EVENING",
                    "현재 분석에서 우선 관리 항목으로 확인된 " + concernLabel(concern) + " 관리를 보조합니다.");
        }
        return List.copyOf(result);
    }

    private void add(List<ProductRecommendationItem> result, Product product,
                     String phase, String reason) {
        if (product != null) result.add(new ProductRecommendationItem(product, phase, reason));
    }

    private String concernLabel(ProductConcern concern) {
        return switch (concern) {
            case WRINKLE -> "주름"; case PORE_TEXTURE -> "모공·피부결";
            case PIGMENTATION -> "색소"; case REDNESS -> "홍조";
            case ACNE -> "여드름"; case BLACKHEAD -> "블랙헤드";
            case DARK_CIRCLE -> "다크서클"; case EYE_SAGGING -> "눈 처짐";
            case SKIN_UNIFORMITY -> "피부 균일도"; default -> "피부 장벽";
        };
    }
}
