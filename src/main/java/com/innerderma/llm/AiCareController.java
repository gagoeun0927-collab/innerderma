package com.innerderma.llm;

import com.innerderma.airule.signal.MappedConcern;
import com.innerderma.airule.solution.SolutionAssembler;
import com.innerderma.airule.solution.SolutionObject;
import com.innerderma.airule.validation.ResponseValidationResult;
import com.innerderma.airule.validation.ResponseValidator;
import com.innerderma.common.response.ApiResponse;
import com.innerderma.knowledge.product.ProductMatchResult;
import com.innerderma.knowledge.product.ProductMatcher;
import com.innerderma.skinstate.domain.SkinStateSnapshot;
import com.innerderma.skinstate.domain.SkinStateSnapshotRepository;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

/**
 * AI 파이프라인 end-to-end 엔드포인트.
 * SignalAssembler → Rule Engine → SolutionObject → Product Matcher → LLM 렌더링 → Response Validator.
 * 기존 CareSolution과 독립적으로 동작한다.
 */
@RestController
@RequestMapping("/api/users/{userCode}/ai-care")
public class AiCareController {

    private final SolutionAssembler solutionAssembler;
    private final ProductMatcher productMatcher;
    private final LlmRenderer llmRenderer;
    private final ResponseValidator responseValidator;
    private final SkinStateSnapshotRepository snapshotRepository;

    public AiCareController(SolutionAssembler solutionAssembler,
                            ProductMatcher productMatcher,
                            LlmRenderer llmRenderer,
                            ResponseValidator responseValidator,
                            SkinStateSnapshotRepository snapshotRepository) {
        this.solutionAssembler = solutionAssembler;
        this.productMatcher = productMatcher;
        this.llmRenderer = llmRenderer;
        this.responseValidator = responseValidator;
        this.snapshotRepository = snapshotRepository;
    }

    @PostMapping
    public ApiResponse<AiCareResponse> generate(
            @PathVariable String userCode,
            @RequestParam(defaultValue = "ko") String locale
    ) {
        // 1. Rule Engine 실행 → Solution Object
        SolutionObject solution = solutionAssembler.assembleForUser(userCode);

        // 2. Primary concern 결정 (Snapshot의 dominant → taxonomy mapping)
        String primaryConcern = resolvePrimaryConcern(userCode);

        // 3. Product Matching
        ProductMatchResult products = productMatcher.match(solution, primaryConcern, null, List.of());

        // 4. LLM 렌더링 (locale 기반 다국어)
        LlmResponse llmResponse = llmRenderer.render(solution, products, locale);

        // 5. Response Validation
        List<String> productIds = extractProductIds(llmResponse);
        int nightSteps = llmResponse.night() != null ? llmResponse.night().steps().size() : 0;
        int morningSteps = llmResponse.morning() != null ? llmResponse.morning().steps().size() : 0;
        int innerCareCount = llmResponse.innerCare() != null ? llmResponse.innerCare().recommended().size() : 0;
        String llmSafety = llmResponse.caution() != null ? "CAUTION" : "NORMAL";

        ResponseValidationResult validation = responseValidator.validate(
                productIds, nightSteps, morningSteps, innerCareCount, llmSafety, solution.actions());

        return ApiResponse.success(new AiCareResponse(
                llmResponse,
                solution.appliedRules(),
                primaryConcern,
                locale,
                validation.valid(),
                validation.violations()
        ));
    }

    private String resolvePrimaryConcern(String userCode) {
        return snapshotRepository.findFirstByUser_UserCodeOrderBySnapshotDateDesc(userCode)
                .map(SkinStateSnapshot::getDominantSymptom)
                .map(MappedConcern::fromSelfReport)
                .map(MappedConcern::concern)
                .orElse("STABLE");
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
