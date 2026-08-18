package com.innerderma.airule.golden;

import com.innerderma.airule.domain.AiRule;
import com.innerderma.airule.domain.AiRuleCategory;
import com.innerderma.airule.domain.AiRuleRepository;
import com.innerderma.airule.engine.RuleEngine;
import com.innerderma.airule.engine.RuleEvaluationContext;
import com.innerderma.airule.engine.RuleEvaluationResult;
import com.innerderma.airule.solution.SolutionAssembler;
import com.innerderma.airule.solution.SolutionObject;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * golden-tests/ 디렉터리의 모든 JSON 파일을 읽어 Rule Engine + Solution Assembler를 검증하는
 * 파라미터화 Golden Test. Rule 변경 시 regression test로 사용한다.
 */
class GoldenRuleEngineTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static RuleEngine ruleEngine;
    private static SolutionAssembler solutionAssembler;

    @BeforeAll
    static void setUp() {
        AiRuleRepository repository = mock(AiRuleRepository.class);
        when(repository.findByEnabledTrueOrderByPriorityDescRuleIdAsc()).thenReturn(seedRules());
        ruleEngine = new RuleEngine(repository, MAPPER);
        solutionAssembler = new SolutionAssembler(mock(com.innerderma.airule.signal.RulePipelineService.class), MAPPER);
    }

    @TestFactory
    Stream<DynamicTest> goldenTests() throws IOException, URISyntaxException {
        Path dir = Paths.get(getClass().getClassLoader().getResource("golden-tests").toURI());
        return Files.list(dir)
                .filter(p -> p.toString().endsWith(".json"))
                .sorted()
                .map(path -> {
                    GoldenTestCase testCase = readCase(path);
                    return DynamicTest.dynamicTest(testCase.id() + ": " + testCase.description(), () -> {
                        RuleEvaluationContext context = RuleEvaluationContext.of(testCase.input().signals());
                        RuleEvaluationResult result = ruleEngine.evaluate(context);
                        SolutionObject solution = solutionAssembler.assemble(result.firedRules(), context);

                        // fired rules
                        if (testCase.expected().fired_rules() != null) {
                            assertThat(result.firedRuleIds())
                                    .as("fired_rules for %s", testCase.id())
                                    .containsAll(testCase.expected().fired_rules());
                        }
                        // not fired rules
                        if (testCase.expected().not_fired_rules() != null) {
                            assertThat(result.firedRuleIds())
                                    .as("not_fired_rules for %s", testCase.id())
                                    .doesNotContainAnyElementsOf(testCase.expected().not_fired_rules());
                        }
                        // actions contain
                        if (testCase.expected().actions_contain() != null) {
                            for (var entry : testCase.expected().actions_contain().entrySet()) {
                                assertThat(solution.actions())
                                        .as("actions[%s] for %s", entry.getKey(), testCase.id())
                                        .containsEntry(entry.getKey(), entry.getValue());
                            }
                        }
                        // restrictions contain
                        if (testCase.expected().restrictions_contain() != null && !testCase.expected().restrictions_contain().isEmpty()) {
                            assertThat(solution.restrictions())
                                    .as("restrictions for %s", testCase.id())
                                    .containsAll(testCase.expected().restrictions_contain());
                        }
                    });
                });
    }

    private GoldenTestCase readCase(Path path) {
        try {
            return MAPPER.readValue(Files.readString(path), GoldenTestCase.class);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to read golden test: " + path, exception);
        }
    }

    private static List<AiRule> seedRules() {
        return List.of(
                rule("R000", AiRuleCategory.SAFETY, 1000,
                        "{\"requires_safety_attention\":true}",
                        "{\"safety_status\":\"CAUTION\",\"recommendation_mode\":\"CONSERVATIVE\",\"require_professional_review_message\":true,\"limit_new_product_addition\":true}",
                        "[\"NO_AGGRESSIVE_ROUTINE\",\"MINIMIZE_PRODUCT_PROMOTION\"]"),
                rule("R002", AiRuleCategory.INPUT_IMAGE, 900,
                        "{\"face_not_detected\":true,\"or_image_blurry\":true}",
                        "{\"request_retake\":true}", "[\"NO_DEFINITIVE_STATE\"]"),
                rule("R020", AiRuleCategory.TREND, 800,
                        "{\"trend_improving\":true}",
                        "{\"recommendation_mode\":\"MAINTENANCE\",\"no_additional_product\":true}",
                        "[\"MAINTAIN_EXISTING_ROUTINE\"]"),
                rule("R021", AiRuleCategory.TREND, 800,
                        "{\"trend_worsening\":true}",
                        "{\"recommendation_mode\":\"CONSERVATIVE\",\"require_safety_reevaluation\":true,\"limit_new_product_addition\":true}",
                        "[\"REEVALUATE_SAFETY\"]"),
                rule("R022", AiRuleCategory.TREND, 700,
                        "{\"trend_stable\":true}",
                        "{\"recommendation_mode\":\"NORMAL\"}", "[]"),
                rule("R023", AiRuleCategory.TREND, 750,
                        "{\"trend_unknown\":true}",
                        "{\"recommendation_mode\":\"CONSERVATIVE\",\"limit_new_product_addition\":true}", "[]"),
                rule("R030", AiRuleCategory.SKIN_STATE, 600,
                        "{\"always\":true}",
                        "{\"concern_source\":\"SELF_REPORT\"}", "[]"),
                rule("R010", AiRuleCategory.PRIORITY_GOAL, 500,
                        "{\"always\":true}",
                        "{\"night_max_steps\":4,\"morning_max_steps\":3,\"inner_care_max_items\":1}",
                        "[\"NO_UNNECESSARY_PRODUCT_ADDITION\"]")
        );
    }

    private static AiRule rule(String ruleId, AiRuleCategory category, int priority,
                               String conditions, String actions, String restrictions) {
        return new AiRule(ruleId, category, ruleId, priority, conditions, actions, restrictions,
                null, "1.0.0", true);
    }
}
