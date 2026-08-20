package com.innerderma.llm;

import com.innerderma.airule.cache.SolutionCache;
import com.innerderma.airule.cache.SolutionCacheEntry;
import com.innerderma.airule.cache.SolutionCacheKey;
import com.innerderma.airule.signal.MappedConcern;
import com.innerderma.airule.solution.SolutionAssembler;
import com.innerderma.airule.solution.SolutionObject;
import com.innerderma.airule.validation.ResponseValidationResult;
import com.innerderma.airule.validation.ResponseValidator;
import com.innerderma.common.response.ApiResponse;
import com.innerderma.knowledge.product.ProductMatchResult;
import com.innerderma.knowledge.product.ProductMatcher;
import com.innerderma.knowledge.product.usage.ProductRecommendationLog;
import com.innerderma.knowledge.product.usage.ProductRecommendationLogRepository;
import com.innerderma.procedure.domain.ProcedureRecord;
import com.innerderma.procedure.domain.ProcedureRecordRepository;
import com.innerderma.skinstate.domain.SkinStateSnapshot;
import com.innerderma.skinstate.domain.SkinStateSnapshotRepository;
import com.innerderma.user.application.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * AI 파이프라인 end-to-end 엔드포인트.
 * SignalAssembler → Rule Engine → SolutionObject → Product Matcher → LLM 렌더링 → Response Validator.
 * 기존 CareSolution과 독립적으로 동작한다.
 *
 * <p>Cache 동작: 같은 날 동일 userCode + scoringVersion + ruleVersion이면 캐시 적중하여
 * LLM 재호출 없이 기존 Solution을 반환한다. 새 분석/문진/시술 등록 시 자동 무효화된다.
 */
@Tag(name = "AI Care", description = "AI 피부 사후관리 파이프라인 — Solution 생성, LLM 렌더링, 캐시 적중")
@RestController
@RequestMapping("/api/users/{userCode}/ai-care")
public class AiCareController {

    private final SolutionAssembler solutionAssembler;
    private final ProductMatcher productMatcher;
    private final LlmRenderer llmRenderer;
    private final ResponseValidator responseValidator;
    private final SkinStateSnapshotRepository snapshotRepository;
    private final ProcedureRecordRepository procedureRecordRepository;
    private final UserService userService;
    private final SolutionCache solutionCache;
    private final ProductRecommendationLogRepository recommendationLogRepository;
    private final com.innerderma.common.demo.DemoUserDataSeeder demoUserDataSeeder;

    public AiCareController(SolutionAssembler solutionAssembler,
                            ProductMatcher productMatcher,
                            LlmRenderer llmRenderer,
                            ResponseValidator responseValidator,
                            SkinStateSnapshotRepository snapshotRepository,
                            ProcedureRecordRepository procedureRecordRepository,
                            UserService userService,
                            SolutionCache solutionCache,
                            ProductRecommendationLogRepository recommendationLogRepository,
                            com.innerderma.common.demo.DemoUserDataSeeder demoUserDataSeeder) {
        this.solutionAssembler = solutionAssembler;
        this.productMatcher = productMatcher;
        this.llmRenderer = llmRenderer;
        this.responseValidator = responseValidator;
        this.snapshotRepository = snapshotRepository;
        this.procedureRecordRepository = procedureRecordRepository;
        this.userService = userService;
        this.solutionCache = solutionCache;
        this.recommendationLogRepository = recommendationLogRepository;
        this.demoUserDataSeeder = demoUserDataSeeder;
    }

    @Operation(
            summary = "AI Care 조회 (부작용 없음)",
            description = "이미 생성된 오늘의 AI 케어를 조회합니다. LLM 호출과 추천 이력 저장을 하지 않습니다. "
                    + "생성된 결과가 없으면 404를 반환하므로, 프론트는 404를 받으면 POST로 생성하면 됩니다. "
                    + "결과는 인메모리 캐시에 보관되므로 서버 재기동 시 사라집니다."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "오늘 생성된 AI 케어가 없음 (POST로 생성 필요)")
    })
    @org.springframework.web.bind.annotation.GetMapping
    public ApiResponse<AiCareResponse> getToday(
            @PathVariable String userCode,
            @RequestParam(required = false) String locale
    ) {
        String resolvedLocale = resolveLocale(userCode, locale);
        SolutionCacheKey cacheKey = buildCacheKey(userCode);

        SolutionCacheEntry entry = solutionCache.get(cacheKey)
                .orElseThrow(() -> new com.innerderma.common.error.BusinessException(
                        com.innerderma.common.error.ErrorCode.AI_CARE_NOT_GENERATED));

        LlmResponse llmResponse = llmRenderer.render(entry.solution(), entry.products(), resolvedLocale);
        return ApiResponse.success(new AiCareResponse(llmResponse, entry.solution().appliedRules(),
                entry.products().primaryConcern(), resolvedLocale, true, List.of(),
                buildProductSources(entry.products())));
    }

    @Operation(summary = "AI Care 생성", description = "피부 상태 기반 AI 케어 솔루션을 생성합니다. 같은 날 동일 조건이면 캐시 결과를 반환합니다.")
    @PostMapping
    public ApiResponse<AiCareResponse> generate(
            @PathVariable String userCode,
            @RequestParam(required = false) String locale
    ) {
        String resolvedLocale = resolveLocale(userCode, locale);

        // 시연용: 자가문진/스냅샷이 없는 신규 사용자에게 최소 데이터를 만들어
        // 파이프라인이 의미 있는 신호를 받을 수 있게 한다. (innerderma.demo.auto-seed-user-data로 제어)
        demoUserDataSeeder.ensureMinimumData(userCode);

        // Cache 적중 확인 (§33 멱등성: 같은 날 동일 조건이면 LLM 재호출 안 함)
        SolutionCacheKey cacheKey = buildCacheKey(userCode);
        var cached = solutionCache.get(cacheKey);
        if (cached.isPresent()) {
            SolutionCacheEntry entry = cached.get();
            LlmResponse llmResponse = llmRenderer.render(entry.solution(), entry.products(), resolvedLocale);
            return ApiResponse.success(new AiCareResponse(llmResponse, entry.solution().appliedRules(),
                    entry.products().primaryConcern(), resolvedLocale, true, List.of(),
                    buildProductSources(entry.products())));
        }

        // 1. Rule Engine 실행 → Solution Object
        SolutionObject solution = solutionAssembler.assembleForUser(userCode);

        // 2. Primary concern 결정 (Snapshot의 dominant → taxonomy mapping)
        String primaryConcern = resolvePrimaryConcern(userCode);

        // 3. Treatment context 자동 판별 (시술 기록 존재 여부)
        String treatmentCode = resolveLatestTreatmentCode(userCode);

        // 4. Product Matching (treatment 유형에 따라 자동 필터)
        ProductMatchResult products = productMatcher.match(solution, primaryConcern, treatmentCode, List.of(), userCode);

        // Cache 저장
        solutionCache.put(cacheKey, new SolutionCacheEntry(solution, products, java.time.LocalDateTime.now()));

        // 추천 이력 저장 (빈도 제한용)
        saveRecommendationLog(userCode, products);

        // 5. LLM 렌더링 (locale 기반 다국어)
        LlmResponse llmResponse = llmRenderer.render(solution, products, resolvedLocale);

        // 6. Response Validation
        List<String> productIds = extractProductIds(llmResponse);
        int nightSteps = llmResponse.night() != null ? llmResponse.night().steps().size() : 0;
        int morningSteps = llmResponse.morning() != null ? llmResponse.morning().steps().size() : 0;
        int innerCareCount = llmResponse.innerCare() != null ? llmResponse.innerCare().recommended().size() : 0;
        String llmSafety = llmResponse.caution() != null ? "CAUTION" : "NORMAL";

        ResponseValidationResult validation = responseValidator.validate(
                productIds, nightSteps, morningSteps, innerCareCount, llmSafety, solution.actions(),
                llmResponse.headline(), llmResponse.skinStateSummary());

        // Validation fail 시 안전한 fallback 응답 (LLM 원본 대신 최소 구조 반환)
        LlmResponse finalResponse = validation.valid() ? llmResponse : buildFallbackResponse(llmResponse, primaryConcern);

        return ApiResponse.success(new AiCareResponse(
                finalResponse,
                solution.appliedRules(),
                primaryConcern,
                resolvedLocale,
                validation.valid(),
                validation.violations(),
                buildProductSources(products)
        ));
    }

    /**
     * Validation 실패 시 LLM 응답의 headline/summary만 보존하고
     * steps/products는 비워서 잘못된 제품이 노출되지 않게 한다.
     */
    private LlmResponse buildFallbackResponse(LlmResponse original, String concern) {
        String headline = (original.headline() != null && !original.headline().isBlank())
                ? original.headline() : "InnerDerma Care";
        String summary = (original.skinStateSummary() != null && !original.skinStateSummary().isBlank())
                ? original.skinStateSummary() : concern;
        return new LlmResponse(
                headline, summary, original.todayGoal(),
                new LlmResponse.NightCare("RECOVERY", List.of()),
                new LlmResponse.MorningCare("PROTECTION", List.of()),
                new LlmResponse.InnerCare(List.of(), List.of()),
                original.caution()
        );
    }

    /** locale 미지정 시 사용자의 preferredLocale 사용 */
    private String resolveLocale(String userCode, String locale) {
        return (locale != null && !locale.isBlank())
                ? locale.trim().toLowerCase()
                : userService.getByUserCode(userCode).getPreferredLocale();
    }

    private SolutionCacheKey buildCacheKey(String userCode) {
        String scoringVersion = snapshotRepository.findFirstByUser_UserCodeOrderBySnapshotDateDesc(userCode)
                .map(SkinStateSnapshot::getScoringVersion).orElse("none");
        return SolutionCacheKey.of(userCode, LocalDate.now(), scoringVersion, "1.0.0");
    }

    private Map<String, String> buildProductSources(ProductMatchResult products) {
        Map<String, String> sources = new HashMap<>();
        products.nightProducts().forEach(p -> sources.put(p.productId(), "PIECE_SEOUL"));
        products.morningProducts().forEach(p -> sources.put(p.productId(), "PIECE_SEOUL"));
        products.innerCareProducts().forEach(p -> sources.put(p.productId(), "WIM_STORE"));
        return sources;
    }

    /**
     * 추천 이력 저장 (제품 추천 빈도 제한용).
     * 같은 날 같은 제품은 한 번만 기록한다 — 캐시가 인메모리라 재배포 후 재호출 시
     * 중복 저장되면 빈도 제한 계산이 왜곡된다.
     */
    private void saveRecommendationLog(String userCode, ProductMatchResult products) {
        var user = userService.getByUserCode(userCode);
        LocalDate today = LocalDate.now();
        products.nightProducts().forEach(p -> saveIfAbsent(user, userCode, p.productId(), "PIECE_SEOUL", today));
        products.morningProducts().forEach(p -> saveIfAbsent(user, userCode, p.productId(), "PIECE_SEOUL", today));
        products.innerCareProducts().forEach(p -> saveIfAbsent(user, userCode, p.productId(), "WIM_STORE", today));
    }

    private void saveIfAbsent(com.innerderma.user.domain.User user, String userCode,
                              String productId, String source, LocalDate date) {
        if (recommendationLogRepository
                .existsByUser_UserCodeAndProductIdAndRecommendedDate(userCode, productId, date)) {
            return;
        }
        recommendationLogRepository.save(new ProductRecommendationLog(user, productId, source, date));
    }

    private String resolvePrimaryConcern(String userCode) {
        return snapshotRepository.findFirstByUser_UserCodeOrderBySnapshotDateDesc(userCode)
                .map(SkinStateSnapshot::getDominantSymptom)
                .map(MappedConcern::fromSelfReport)
                .map(MappedConcern::concern)
                .orElse("STABLE");
    }

    /**
     * 사용자 유형 자동 판별 (정책 A): 시술 기록이 존재하면 최신 시술의 procedureName을
     * Treatment KB에서 매칭해 treatmentCode를 반환.
     * 없으면 null → Product Matcher에서 treatment 필터 미적용 (Diagnosis Only 유형).
     */
    private String resolveLatestTreatmentCode(String userCode) {
        return procedureRecordRepository
                .findFirstByUser_UserCodeAndProcedureDateLessThanEqualOrderByProcedureDateDesc(
                        userCode, LocalDate.now())
                .map(ProcedureRecord::getProcedureName)
                .orElse(null);
    }

    private List<String> extractProductIds(LlmResponse response) {
        List<String> ids = new ArrayList<>();
        if (response.night() != null && response.night().steps() != null) {
            response.night().steps().forEach(s -> ids.add(s.productId()));
        }
        if (response.morning() != null && response.morning().steps() != null) {
            response.morning().steps().forEach(s -> ids.add(s.productId()));
        }
        if (response.innerCare() != null && response.innerCare().recommended() != null) {
            response.innerCare().recommended().forEach(r -> ids.add(r.productId()));
        }
        return ids;
    }
}
