package com.innerderma.airule.signal;

import com.innerderma.airule.engine.RuleEngine;
import com.innerderma.airule.engine.RuleEvaluationResult;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 신호 조립 → Rule Engine 실행을 연결하는 파이프라인. SignalAssembler 가 만든 결정적 신호로
 * RuleEngine 을 실행한다. 규칙 실행 순서/그룹핑은 RuleEngine 이 category·priority 로만 결정하고,
 * ruleId 는 참조 식별자로만 쓰인다.
 */
@Service
@Transactional(readOnly = true)
public class RulePipelineService {

    private final SignalAssembler signalAssembler;
    private final RuleEngine ruleEngine;

    public RulePipelineService(SignalAssembler signalAssembler, RuleEngine ruleEngine) {
        this.signalAssembler = signalAssembler;
        this.ruleEngine = ruleEngine;
    }

    public RuleEvaluationResult evaluateForUser(String userCode) {
        return runForUser(userCode).result();
    }

    /** 신호 컨텍스트와 발화 규칙을 함께 반환해 후속 단계(Solution 조립 등)가 재실행 없이 재사용하도록 한다. */
    public RulePipelineOutcome runForUser(String userCode) {
        var context = signalAssembler.assemble(userCode);
        return new RulePipelineOutcome(context, ruleEngine.evaluate(context));
    }
}
