package com.innerderma.productrecommendation.application;

import com.innerderma.carecycle.domain.CareCycle;
import com.innerderma.caresolution.domain.*;
import com.innerderma.product.domain.*;
import com.innerderma.skinanalysis.domain.SkinAnalysis;
import com.innerderma.skincapture.domain.SkinCapture;
import com.innerderma.skincapture.domain.SkinCaptureQualityStatus;
import com.innerderma.user.domain.User;
import com.innerderma.user.domain.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.*;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ProductRecommendationServiceTest {
    private static final String USER_CODE = "WHS-DEMO-001";
    private CareSolutionRepository solutionRepository;
    private ProductRepository productRepository;
    private UserRepository userRepository;
    private ProductRecommendationService service;

    @BeforeEach
    void setUp() {
        solutionRepository = mock(CareSolutionRepository.class);
        productRepository = mock(ProductRepository.class);
        userRepository = mock(UserRepository.class);
        service = new ProductRecommendationService(solutionRepository, productRepository,
                userRepository, Clock.fixed(Instant.parse("2026-08-17T03:30:00Z"),
                ZoneId.of("Asia/Seoul")));
        when(userRepository.existsByUserCode(USER_CODE)).thenReturn(true);
        when(productRepository.findAllByActiveTrueOrderByDisplayPriorityAscProductCodeAsc())
                .thenReturn(products());
    }

    @Test
    void recommendsBaseProductsAndMatchingConcernProduct() {
        CareSolution solution = solution(SafetyLevel.NORMAL, "redness");
        stubSolution(solution, LocalDate.of(2026, 8, 17));

        ProductRecommendationResult result = service.getDaily(USER_CODE, null);

        assertThat(result.items()).extracting(item -> item.product().getProductCode())
                .containsExactly("CLEANSER", "MOISTURIZER", "SUNSCREEN", "REDNESS");
        assertThat(result.items()).noneMatch(item -> item.product().getProductCode().equals("WRINKLE"));
        assertThat(result.safetyLevel()).isEqualTo(SafetyLevel.NORMAL);
    }

    @Test
    void safetyAttentionExcludesAllTargetedCare() {
        CareSolution solution = solution(SafetyLevel.ATTENTION, "redness");
        stubSolution(solution, LocalDate.of(2026, 8, 19));

        ProductRecommendationResult result = service.getDaily(USER_CODE, LocalDate.of(2026, 8, 19));

        assertThat(result.items()).extracting(item -> item.product().getCategory())
                .containsExactly(ProductCategory.CLEANSER, ProductCategory.MOISTURIZER,
                        ProductCategory.SUNSCREEN);
        assertThat(result.items()).allMatch(item -> item.product().isSafetyAttentionCompatible());
        assertThat(result.notice()).contains("의료진 문의를 우선");
        assertThat(result.inherited()).isTrue();
    }

    private void stubSolution(CareSolution solution, LocalDate servedDate) {
        when(solutionRepository.findFirstByCareCycle_User_UserCodeAndCareCycle_OriginCaptureDateLessThanEqualOrderByCareCycle_OriginCaptureDateDescGeneratedAtDesc(
                USER_CODE, servedDate)).thenReturn(Optional.of(solution));
    }

    private List<Product> products() {
        return List.of(
                product("CLEANSER", ProductCategory.CLEANSER, ProductConcern.GENERAL, true, 1),
                product("MOISTURIZER", ProductCategory.MOISTURIZER, ProductConcern.GENERAL, true, 2),
                product("SUNSCREEN", ProductCategory.SUNSCREEN, ProductConcern.GENERAL, true, 3),
                product("WRINKLE", ProductCategory.TARGETED_CARE, ProductConcern.WRINKLE, false, 4),
                product("REDNESS", ProductCategory.TARGETED_CARE, ProductConcern.REDNESS, false, 5)
        );
    }

    private Product product(String code, ProductCategory category, ProductConcern concern,
                            boolean compatible, int priority) {
        return new Product(code, "[데모]", code, category, concern, compatible,
                true, true, null, priority);
    }

    private CareSolution solution(SafetyLevel safetyLevel, String concern) {
        User user = new User(USER_CODE, "테스트 사용자", "010-1234-1234");
        LocalDate date = LocalDate.of(2026, 8, 17);
        SkinCapture capture = new SkinCapture(user, date, date.atTime(10, 0), "/face.jpg",
                "face.jpg", "image/jpeg", 3, SkinCaptureQualityStatus.VALID);
        SkinAnalysis analysis = new SkinAnalysis(capture, date.atTime(10, 1), 70,
                "Good", "1.0", "{}");
        CareCycle cycle = new CareCycle(user, analysis, null, date, date.atTime(10, 2));
        return new CareSolution(cycle, null, null, CareSeason.SUMMER, safetyLevel,
                "안내", "[]", "[]", null, concern, date.atTime(10, 3));
    }
}
