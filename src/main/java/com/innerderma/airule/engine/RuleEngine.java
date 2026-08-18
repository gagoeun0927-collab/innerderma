package com.innerderma.airule.engine;

import com.innerderma.airule.domain.AiRule;
import com.innerderma.airule.domain.AiRuleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.util.Comparator;
import java.util.Map;

/**
 * 결정적 Rule Engine 실행기.
 *
 * <p>Rule ID 채번 정책(A)에 따라 {@code ruleId} 는 안정적인 참조 식별자로만 쓰고, 실행 순서와 그룹핑은
 * {@code category}(파이프라인 단계, enum 선언 순서) 와 {@code priority}(같은 단계 내 우선순위) 로만 결정한다.
 *
 * <p>조건 평가 규약: 규칙의 conditions JSON 에서
 * <ul>
 *   <li>{@code {"always": true}} 이면 항상 발화한다.</li>
 *   <li>그 외에는 나열된 flag 중 하나라도 컨텍스트에서 활성(true)이면 발화한다(OR). 이는 현재 원장에 시드된
 *       규칙(R000/R002/R010)의 구조와 일치한다. 중첩 AND/OR 등 더 복잡한 표현식은 향후 확장 대상이다.</li>
 *   <li>conditions JSON 이 손상되어 파싱할 수 없으면 해당 규칙은 발화하지 않고 건너뛴다.</li>
 * </ul>
 */
@Service
@Transactional(readOnly = true)
public class RuleEngine {

    private static final String ALWAYS = "always";

    private static final Comparator<AiRule> PIPELINE_ORDER = Comparator
            .comparingInt((AiRule rule) -> rule.getCategory().ordinal())
            .thenComparing(Comparator.comparingInt(AiRule::getPriority).reversed());

    private final AiRuleRepository ruleRepository;
    private final ObjectMapper objectMapper;

    public RuleEngine(AiRuleRepository ruleRepository, ObjectMapper objectMapper) {
        this.ruleRepository = ruleRepository;
        this.objectMapper = objectMapper;
    }

    public RuleEvaluationResult evaluate(RuleEvaluationContext context) {
        var fired = ruleRepository.findByEnabledTrueOrderByPriorityDescRuleIdAsc().stream()
                .filter(rule -> matches(rule, context))
                .sorted(PIPELINE_ORDER)
                .toList();
        return new RuleEvaluationResult(fired);
    }

    private boolean matches(AiRule rule, RuleEvaluationContext context) {
        Map<String, Object> conditions = parseConditions(rule.getConditionsJson());
        if (conditions == null) {
            return false;
        }
        if (Boolean.TRUE.equals(conditions.get(ALWAYS))) {
            return true;
        }
        for (Map.Entry<String, Object> entry : conditions.entrySet()) {
            if (ALWAYS.equals(entry.getKey())) {
                continue;
            }
            if (Boolean.TRUE.equals(entry.getValue()) && context.isActive(entry.getKey())) {
                return true;
            }
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseConditions(String conditionsJson) {
        if (conditionsJson == null || conditionsJson.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(conditionsJson, Map.class);
        } catch (JacksonException exception) {
            return null;
        }
    }
}
