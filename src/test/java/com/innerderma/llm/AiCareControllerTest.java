package com.innerderma.llm;

import com.innerderma.airule.cache.SolutionCache;
import com.innerderma.airule.signal.MappedConcern;
import com.innerderma.airule.solution.SolutionAssembler;
import com.innerderma.airule.solution.SolutionObject;
import com.innerderma.airule.validation.ResponseValidationResult;
import com.innerderma.airule.validation.ResponseValidator;
import com.innerderma.common.error.GlobalExceptionHandler;
import com.innerderma.knowledge.product.PieceSeoulProduct;
import com.innerderma.knowledge.product.ProductMatchResult;
import com.innerderma.knowledge.product.ProductMatcher;
import com.innerderma.knowledge.product.WimStoreProduct;
import com.innerderma.skinstate.domain.SkinStateSnapshot;
import com.innerderma.skinstate.domain.SkinStateSnapshotRepository;
import com.innerderma.user.domain.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AiCareControllerTest {

    private SolutionAssembler solutionAssembler;
    private ProductMatcher productMatcher;
    private LlmRenderer llmRenderer;
    private ResponseValidator responseValidator;
    private SkinStateSnapshotRepository snapshotRepository;
    private com.innerderma.procedure.domain.ProcedureRecordRepository procedureRecordRepository;
    private com.innerderma.user.application.UserService userService;
    private SolutionCache solutionCache;
    private com.innerderma.common.demo.DemoUserDataSeeder demoUserDataSeeder;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        solutionAssembler = mock(SolutionAssembler.class);
        productMatcher = mock(ProductMatcher.class);
        llmRenderer = mock(LlmRenderer.class);
        responseValidator = mock(ResponseValidator.class);
        snapshotRepository = mock(SkinStateSnapshotRepository.class);
        procedureRecordRepository = mock(com.innerderma.procedure.domain.ProcedureRecordRepository.class);
        userService = mock(com.innerderma.user.application.UserService.class);
        solutionCache = mock(SolutionCache.class);
        demoUserDataSeeder = mock(com.innerderma.common.demo.DemoUserDataSeeder.class);
        when(solutionCache.get(any())).thenReturn(Optional.empty());
        mockMvc = MockMvcBuilders.standaloneSetup(
                        new AiCareController(solutionAssembler, productMatcher, llmRenderer, responseValidator, snapshotRepository, procedureRecordRepository, userService, solutionCache, mock(com.innerderma.knowledge.product.usage.ProductRecommendationLogRepository.class), demoUserDataSeeder))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        when(procedureRecordRepository.findFirstByUser_UserCodeAndProcedureDateLessThanEqualOrderByProcedureDateDesc(any(), any()))
                .thenReturn(java.util.Optional.empty());
    }

    @Test
    void returnsFullAiCareResponseWithProducts() throws Exception {
        // Solution
        SolutionObject solution = new SolutionObject(
                Map.of("night_max_steps", 4, "recommendation_mode", "NORMAL"),
                List.of(), List.of("R022@1.0.0", "R030@1.0.0", "R010@1.0.0"), List.of(), Map.of(), Map.of());
        when(solutionAssembler.assembleForUser("WHS-DEMO-001")).thenReturn(solution);

        // Snapshot → concern
        SkinStateSnapshot snapshot = new SkinStateSnapshot(
                new User("WHS-DEMO-001", "test", "010-1234-1234"),
                LocalDate.of(2026, 8, 19), "selfcheck-ordinal-v1", "{}", null, "dryness", 1L, null,
                LocalDateTime.now());
        when(snapshotRepository.findFirstByUser_UserCodeOrderBySnapshotDateDesc("WHS-DEMO-001"))
                .thenReturn(Optional.of(snapshot));

        // Product matching
        PieceSeoulProduct nightProduct = new PieceSeoulProduct(
                "PSS_001", "Piece Seoul", "Cica Cream", "MOISTURIZER",
                List.of("barrier"), List.of("HYDRATION"), List.of(), List.of(),
                List.of("night"), "daily", "fingertip", "얼굴 전체 도포",
                List.of(), List.of("장벽 강화"), List.of(), List.of(), true, 38000, null, null, null);
        WimStoreProduct innerProduct = new WimStoreProduct(
                "WIM_001", "WIM", "콜라겐 젤리", "JELLY",
                List.of("HYDRATION"), List.of(), List.of(), List.of(),
                "1일 1포", List.of("콜라겐 함유"), List.of(), List.of(), true, 45000, null, null, null);
        when(productMatcher.match(any(), eq("HYDRATION"), any(), any(), any()))
                .thenReturn(new ProductMatchResult(List.of(nightProduct), List.of(), List.of(innerProduct), "HYDRATION", null));

        // LLM response
        LlmResponse llmResponse = new LlmResponse(
                "오늘의 스킨케어",
                "수분 부족 상태",
                "보습 강화",
                new LlmResponse.NightCare("RECOVERY", List.of(
                        new LlmResponse.Step(1, "PSS_001", "Cica Cream", "얼굴 전체 도포", "장벽 강화"))),
                new LlmResponse.MorningCare("PROTECTION", List.of()),
                new LlmResponse.InnerCare(List.of(
                        new LlmResponse.Recommendation("WIM_001", "콜라겐 젤리", "1일 1포", "콜라겐 보충")), List.of()),
                null);
        when(llmRenderer.render(any(), any(), eq("ko"))).thenReturn(llmResponse);

        // Validation
        when(responseValidator.validate(any(), any(int.class), any(int.class), any(int.class), any(), any(), any(), any()))
                .thenReturn(ResponseValidationResult.success());

        mockMvc.perform(post("/api/users/WHS-DEMO-001/ai-care").param("locale", "ko"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.care.headline").value("오늘의 스킨케어"))
                .andExpect(jsonPath("$.data.care.skinStateSummary").value("수분 부족 상태"))
                .andExpect(jsonPath("$.data.care.night.steps[0].productId").value("PSS_001"))
                .andExpect(jsonPath("$.data.care.innerCare.recommended[0].productId").value("WIM_001"))
                .andExpect(jsonPath("$.data.appliedRules[0]").value("R022@1.0.0"))
                .andExpect(jsonPath("$.data.primaryConcern").value("HYDRATION"))
                .andExpect(jsonPath("$.data.locale").value("ko"))
                .andExpect(jsonPath("$.data.validated").value(true));
    }

    @Test
    void returnsSafetyResponseWithCaution() throws Exception {
        SolutionObject solution = new SolutionObject(
                Map.of("safety_status", "CAUTION", "recommendation_mode", "CONSERVATIVE", "limit_new_product_addition", true),
                List.of("NO_AGGRESSIVE_ROUTINE"), List.of("R000@1.0.0", "R010@1.0.0"), List.of(), Map.of(), Map.of());
        when(solutionAssembler.assembleForUser("WHS-DEMO-001")).thenReturn(solution);
        when(snapshotRepository.findFirstByUser_UserCodeOrderBySnapshotDateDesc("WHS-DEMO-001"))
                .thenReturn(Optional.empty());
        when(productMatcher.match(any(), any(), any(), any(), any()))
                .thenReturn(new ProductMatchResult(List.of(), List.of(), List.of(), "STABLE", null));

        LlmResponse llmResponse = new LlmResponse(
                "주의 안내", "피부 상태 주의", "안정화",
                new LlmResponse.NightCare("RECOVERY", List.of()),
                new LlmResponse.MorningCare("PROTECTION", List.of()),
                new LlmResponse.InnerCare(List.of(), List.of()),
                "현재 주의가 필요한 상태입니다.");
        when(llmRenderer.render(any(), any(), eq("en"))).thenReturn(llmResponse);
        when(responseValidator.validate(any(), any(int.class), any(int.class), any(int.class), any(), any(), any(), any()))
                .thenReturn(ResponseValidationResult.success());

        mockMvc.perform(post("/api/users/WHS-DEMO-001/ai-care").param("locale", "en"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.care.caution").value("현재 주의가 필요한 상태입니다."))
                .andExpect(jsonPath("$.data.appliedRules[0]").value("R000@1.0.0"));
    }

    @Test
    void defaultsToStableWhenNoSnapshot() throws Exception {
        SolutionObject solution = new SolutionObject(
                Map.of(), List.of(), List.of("R010@1.0.0"), List.of(), Map.of(), Map.of());
        when(solutionAssembler.assembleForUser("WHS-DEMO-001")).thenReturn(solution);
        when(snapshotRepository.findFirstByUser_UserCodeOrderBySnapshotDateDesc("WHS-DEMO-001"))
                .thenReturn(Optional.empty());
        when(productMatcher.match(any(), eq("STABLE"), any(), any(), any()))
                .thenReturn(new ProductMatchResult(List.of(), List.of(), List.of(), "STABLE", null));

        // locale 미지정 → userService에서 preferredLocale 조회
        com.innerderma.user.domain.User user = new com.innerderma.user.domain.User("WHS-DEMO-001", "test", "010-1234-1234");
        user.updatePreferredLocale("ko");
        when(userService.getByUserCode("WHS-DEMO-001")).thenReturn(user);

        LlmResponse llmResponse = new LlmResponse("Care", "STABLE", "", null, null, null, null);
        when(llmRenderer.render(any(), any(), any())).thenReturn(llmResponse);
        when(responseValidator.validate(any(), any(int.class), any(int.class), any(int.class), any(), any(), any(), any()))
                .thenReturn(ResponseValidationResult.success());

        mockMvc.perform(post("/api/users/WHS-DEMO-001/ai-care"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.primaryConcern").value("STABLE"))
                .andExpect(jsonPath("$.data.locale").value("ko"));
    }

    @Test
    void getReturnsNotFoundWhenNothingGeneratedToday() throws Exception {
        when(snapshotRepository.findFirstByUser_UserCodeOrderBySnapshotDateDesc("WHS-DEMO-001"))
                .thenReturn(Optional.empty());
        com.innerderma.user.domain.User user =
                new com.innerderma.user.domain.User("WHS-DEMO-001", "test", "010-1234-1234");
        user.updatePreferredLocale("ko");
        when(userService.getByUserCode("WHS-DEMO-001")).thenReturn(user);
        when(solutionCache.get(any())).thenReturn(Optional.empty());

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .get("/api/users/WHS-DEMO-001/ai-care"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("AI_CARE_001"));
    }

    @Test
    void getReturnsCachedResultWithoutRegenerating() throws Exception {
        SolutionObject solution = new SolutionObject(
                Map.of(), List.of(), List.of("R010@1.0.0"), List.of(), Map.of(), Map.of());
        ProductMatchResult products =
                new ProductMatchResult(List.of(), List.of(), List.of(), "HYDRATION", null);
        when(solutionCache.get(any())).thenReturn(Optional.of(
                new com.innerderma.airule.cache.SolutionCacheEntry(solution, products, LocalDateTime.now())));
        when(snapshotRepository.findFirstByUser_UserCodeOrderBySnapshotDateDesc("WHS-DEMO-001"))
                .thenReturn(Optional.empty());
        com.innerderma.user.domain.User user =
                new com.innerderma.user.domain.User("WHS-DEMO-001", "test", "010-1234-1234");
        user.updatePreferredLocale("ko");
        when(userService.getByUserCode("WHS-DEMO-001")).thenReturn(user);

        LlmResponse llmResponse = new LlmResponse("Cached Care", "HYDRATION", "", null, null, null, null);
        when(llmRenderer.render(any(), any(), any())).thenReturn(llmResponse);

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .get("/api/users/WHS-DEMO-001/ai-care"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.care.headline").value("Cached Care"))
                .andExpect(jsonPath("$.data.primaryConcern").value("HYDRATION"));

        // GET은 Rule Engine을 재실행하지 않는다
        org.mockito.Mockito.verify(solutionAssembler, org.mockito.Mockito.never()).assembleForUser(any());
    }

    @Test
    void doesNotDuplicateRecommendationLogForSameProductSameDay() throws Exception {
        var logRepository = mock(com.innerderma.knowledge.product.usage.ProductRecommendationLogRepository.class);
        mockMvc = MockMvcBuilders.standaloneSetup(
                        new AiCareController(solutionAssembler, productMatcher, llmRenderer, responseValidator,
                                snapshotRepository, procedureRecordRepository, userService, solutionCache,
                                logRepository, demoUserDataSeeder))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        SolutionObject solution = new SolutionObject(
                Map.of(), List.of(), List.of("R010@1.0.0"), List.of(), Map.of(), Map.of());
        when(solutionAssembler.assembleForUser("WHS-DEMO-001")).thenReturn(solution);
        when(snapshotRepository.findFirstByUser_UserCodeOrderBySnapshotDateDesc("WHS-DEMO-001"))
                .thenReturn(Optional.empty());

        PieceSeoulProduct nightProduct = new PieceSeoulProduct(
                "PSS_001", "Piece Seoul", "Cica Cream", "MOISTURIZER",
                List.of("barrier"), List.of("HYDRATION"), List.of(), List.of(),
                List.of("night"), "daily", "fingertip", "얼굴 전체 도포",
                List.of(), List.of("장벽 강화"), List.of(), List.of(), true, 38000, null, null, null);
        when(productMatcher.match(any(), any(), any(), any(), any()))
                .thenReturn(new ProductMatchResult(List.of(nightProduct), List.of(), List.of(), "STABLE", null));

        com.innerderma.user.domain.User user =
                new com.innerderma.user.domain.User("WHS-DEMO-001", "test", "010-1234-1234");
        user.updatePreferredLocale("ko");
        when(userService.getByUserCode("WHS-DEMO-001")).thenReturn(user);
        when(llmRenderer.render(any(), any(), any()))
                .thenReturn(new LlmResponse("Care", "STABLE", "", null, null, null, null));
        when(responseValidator.validate(any(), any(int.class), any(int.class), any(int.class), any(), any(), any(), any()))
                .thenReturn(ResponseValidationResult.success());

        // 이미 오늘 기록이 있다고 응답 → 저장하지 않아야 한다
        when(logRepository.existsByUser_UserCodeAndProductIdAndRecommendedDate(
                eq("WHS-DEMO-001"), eq("PSS_001"), any())).thenReturn(true);

        mockMvc.perform(post("/api/users/WHS-DEMO-001/ai-care"))
                .andExpect(status().isOk());

        org.mockito.Mockito.verify(logRepository, org.mockito.Mockito.never()).save(any());
    }

    @Test
    void postSeedsMinimumDataForNewUser() throws Exception {
        SolutionObject solution = new SolutionObject(
                Map.of(), List.of(), List.of("R010@1.0.0"), List.of(), Map.of(), Map.of());
        when(solutionAssembler.assembleForUser("NEW-USER-001")).thenReturn(solution);
        when(snapshotRepository.findFirstByUser_UserCodeOrderBySnapshotDateDesc("NEW-USER-001"))
                .thenReturn(Optional.empty());
        when(productMatcher.match(any(), any(), any(), any(), any()))
                .thenReturn(new ProductMatchResult(List.of(), List.of(), List.of(), "STABLE", null));
        com.innerderma.user.domain.User user =
                new com.innerderma.user.domain.User("NEW-USER-001", "new", "010-0000-0000");
        user.updatePreferredLocale("ko");
        when(userService.getByUserCode("NEW-USER-001")).thenReturn(user);
        when(llmRenderer.render(any(), any(), any()))
                .thenReturn(new LlmResponse("Care", "STABLE", "", null, null, null, null));
        when(responseValidator.validate(any(), any(int.class), any(int.class), any(int.class), any(), any(), any(), any()))
                .thenReturn(ResponseValidationResult.success());

        mockMvc.perform(post("/api/users/NEW-USER-001/ai-care"))
                .andExpect(status().isOk());

        org.mockito.Mockito.verify(demoUserDataSeeder).ensureMinimumData("NEW-USER-001");
    }

    @Test
    void getDoesNotSeedData() throws Exception {
        when(snapshotRepository.findFirstByUser_UserCodeOrderBySnapshotDateDesc("NEW-USER-001"))
                .thenReturn(Optional.empty());
        com.innerderma.user.domain.User user =
                new com.innerderma.user.domain.User("NEW-USER-001", "new", "010-0000-0000");
        user.updatePreferredLocale("ko");
        when(userService.getByUserCode("NEW-USER-001")).thenReturn(user);
        when(solutionCache.get(any())).thenReturn(Optional.empty());

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .get("/api/users/NEW-USER-001/ai-care"))
                .andExpect(status().isNotFound());

        // GET은 읽기 전용이어야 한다
        org.mockito.Mockito.verify(demoUserDataSeeder, org.mockito.Mockito.never()).ensureMinimumData(any());
    }

    @Test
    void marksRoutineWithheldWhenRuleLimitsNewProducts() throws Exception {
        // R001(시술 회복기)이 limit_new_product_addition을 적용한 상황
        SolutionObject solution = new SolutionObject(
                Map.of("limit_new_product_addition", true, "recommendation_mode", "CONSERVATIVE"),
                List.of("NO_AGGRESSIVE_ROUTINE"),
                List.of("R001@1.0.0", "R010@1.0.0"),
                List.of(),
                Map.of("R001", "시술 후 회복 기간입니다. 시술 aftercare 지침을 우선 적용합니다."),
                Map.of());
        when(solutionAssembler.assembleForUser("WHS-DEMO-001")).thenReturn(solution);
        when(snapshotRepository.findFirstByUser_UserCodeOrderBySnapshotDateDesc("WHS-DEMO-001"))
                .thenReturn(Optional.empty());
        // limit_new_product_addition이면 ProductMatcher가 빈 목록을 반환한다
        when(productMatcher.match(any(), any(), any(), any(), any()))
                .thenReturn(new ProductMatchResult(List.of(), List.of(), List.of(), "STABLE", null));
        com.innerderma.user.domain.User user =
                new com.innerderma.user.domain.User("WHS-DEMO-001", "test", "010-1234-1234");
        user.updatePreferredLocale("ko");
        when(userService.getByUserCode("WHS-DEMO-001")).thenReturn(user);
        when(llmRenderer.render(any(), any(), any()))
                .thenReturn(new LlmResponse("Care", "STABLE", "", null, null, null, null));
        when(responseValidator.validate(any(), any(int.class), any(int.class), any(int.class), any(), any(), any(), any()))
                .thenReturn(ResponseValidationResult.success());

        mockMvc.perform(post("/api/users/WHS-DEMO-001/ai-care"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.routineWithheld").value(true))
                .andExpect(jsonPath("$.data.routineWithheldReason")
                        .value("시술 후 회복 기간입니다. 시술 aftercare 지침을 우선 적용합니다."));
    }

    @Test
    void doesNotMarkWithheldWhenProductsAreProvided() throws Exception {
        SolutionObject solution = new SolutionObject(
                Map.of("night_max_steps", 4), List.of(), List.of("R010@1.0.0"), List.of(), Map.of(), Map.of());
        when(solutionAssembler.assembleForUser("WHS-DEMO-001")).thenReturn(solution);
        when(snapshotRepository.findFirstByUser_UserCodeOrderBySnapshotDateDesc("WHS-DEMO-001"))
                .thenReturn(Optional.empty());
        PieceSeoulProduct nightProduct = new PieceSeoulProduct(
                "PSS_001", "Piece Seoul", "Cica Cream", "MOISTURIZER",
                List.of("barrier"), List.of("HYDRATION"), List.of(), List.of(),
                List.of("night"), "daily", "fingertip", "얼굴 전체 도포",
                List.of(), List.of("장벽 강화"), List.of(), List.of(), true, 38000, null, null, null);
        when(productMatcher.match(any(), any(), any(), any(), any()))
                .thenReturn(new ProductMatchResult(List.of(nightProduct), List.of(), List.of(), "HYDRATION", null));
        com.innerderma.user.domain.User user =
                new com.innerderma.user.domain.User("WHS-DEMO-001", "test", "010-1234-1234");
        user.updatePreferredLocale("ko");
        when(userService.getByUserCode("WHS-DEMO-001")).thenReturn(user);
        when(llmRenderer.render(any(), any(), any()))
                .thenReturn(new LlmResponse("Care", "HYDRATION", "", null, null, null, null));
        when(responseValidator.validate(any(), any(int.class), any(int.class), any(int.class), any(), any(), any(), any()))
                .thenReturn(ResponseValidationResult.success());

        mockMvc.perform(post("/api/users/WHS-DEMO-001/ai-care"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.routineWithheld").value(false))
                .andExpect(jsonPath("$.data.routineWithheldReason").doesNotExist());
    }
}
