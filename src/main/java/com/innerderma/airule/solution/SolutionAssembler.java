package com.innerderma.airule.solution;

import com.innerderma.airule.domain.AiRule;
import com.innerderma.airule.engine.RuleEvaluationContext;
import com.innerderma.airule.signal.RulePipelineOutcome;
import com.innerderma.airule.signal.RulePipelineService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 발화 규칙만 입력으로 받아 비영속 Solution Object 를 결정적으로 조립한다.
 *
 * <p>병합 규칙:
 * <ul>
 *   <li>actions: 실행 순서(RuleEngine 이 category pipeline → priority desc 로 정렬)대로 병합.
 *       숫자 제한 값(예: night_max_steps)은 규칙에 명시된 경우에만 가장 제한적인 값(최솟값)을 적용한다.
 *       숫자가 아닌 값이 충돌하면 먼저 적용된(높은 우선순위) 값을 유지하고 충돌 정보를 보존한다.</li>
 *   <li>restrictions: 상충하지 않는 한 union, 중복 제거, 첫 등장 순서 유지.</li>
 *   <li>appliedRules: {@code ruleId@version} 형식, 실행 순서 유지.</li>
 *   <li>explanationTemplate: 규칙이 제공한 값만 보존(신규 문구 생성 없음).</li>
 * </ul>
 * 규칙에 없는 상한/판단을 새로 만들지 않는다.
 */
@Service
@Transactional(readOnly = true)
public class SolutionAssembler {

    private final RulePipelineService pipelineService;
    private final ObjectMapper objectMapper;

    public SolutionAssembler(RulePipelineService pipelineService, ObjectMapper objectMapper) {
        this.pipelineService = pipelineService;
        this.objectMapper = objectMapper;
    }

    public SolutionObject assembleForUser(String userCode) {
        RulePipelineOutcome outcome = pipelineService.runForUser(userCode);
        return assemble(outcome.result().firedRules(), outcome.context());
    }

    public SolutionObject assemble(List<AiRule> firedRules, RuleEvaluationContext context) {
        Map<String, Object> actions = new LinkedHashMap<>();
        Map<String, String> actionSetBy = new LinkedHashMap<>();
        List<ActionConflict> conflicts = new ArrayList<>();
        LinkedHashSet<String> restrictions = new LinkedHashSet<>();
        List<String> appliedRules = new ArrayList<>();
        Map<String, String> explanationTemplates = new LinkedHashMap<>();

        for (AiRule rule : firedRules) {
            appliedRules.add(rule.getRuleId() + "@" + rule.getVersion());
            if (rule.getExplanationTemplate() != null && !rule.getExplanationTemplate().isBlank()) {
                explanationTemplates.put(rule.getRuleId(), rule.getExplanationTemplate());
            }
            mergeActions(actions, actionSetBy, conflicts, rule);
            mergeRestrictions(restrictions, rule.getRestrictionsJson());
        }

        return new SolutionObject(actions, new ArrayList<>(restrictions), appliedRules,
                conflicts, explanationTemplates, context.signals());
    }

    private void mergeActions(Map<String, Object> actions, Map<String, String> actionSetBy,
                              List<ActionConflict> conflicts, AiRule rule) {
        Map<String, Object> ruleActions = parseObject(rule.getActionsJson());
        for (Map.Entry<String, Object> entry : ruleActions.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            if (!actions.containsKey(key)) {
                actions.put(key, value);
                actionSetBy.put(key, rule.getRuleId());
                continue;
            }
            Object existing = actions.get(key);
            if (existing instanceof Number existingNumber && value instanceof Number incomingNumber) {
                // 숫자 제한: 가장 제한적인 값(최솟값) 적용 (규칙에 명시된 경우에만).
                if (incomingNumber.doubleValue() < existingNumber.doubleValue()) {
                    actions.put(key, value);
                    actionSetBy.put(key, rule.getRuleId());
                }
            } else if (!Objects.equals(existing, value)) {
                // 숫자가 아닌 충돌: 먼저 적용된 값 유지 + 충돌 보존.
                conflicts.add(new ActionConflict(key, existing, value,
                        actionSetBy.get(key), rule.getRuleId()));
            }
        }
    }

    private void mergeRestrictions(LinkedHashSet<String> restrictions, String restrictionsJson) {
        for (Object item : parseArray(restrictionsJson)) {
            if (item != null) {
                restrictions.add(item.toString());
            }
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseObject(String json) {
        if (json == null || json.isBlank()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(json, Map.class);
        } catch (JacksonException exception) {
            return Map.of();
        }
    }

    private List<Object> parseArray(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, List.class);
        } catch (JacksonException exception) {
            return List.of();
        }
    }
}
