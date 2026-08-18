package com.innerderma.airule.api;

import com.innerderma.airule.application.AiRuleService;
import com.innerderma.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "AI Rule", description = "AI 규칙 관리 API — 규칙 조회, 활성화/비활성화 토글")
@RestController
@RequestMapping("/api/ai-rules")
public class AiRuleController {
    private final AiRuleService service;

    public AiRuleController(AiRuleService service) {
        this.service = service;
    }

    @Operation(summary = "전체 규칙 목록 조회", description = "활성/비활성 포함 모든 AI 규칙을 반환합니다.")
    @GetMapping
    public ApiResponse<List<AiRuleResponse>> getAllRules() {
        return ApiResponse.success(service.getAllRules().stream().map(AiRuleResponse::from).toList());
    }

    @Operation(summary = "활성 규칙만 조회", description = "현재 enabled=true인 규칙만 priority DESC 순으로 반환합니다.")
    @GetMapping("/enabled")
    public ApiResponse<List<AiRuleResponse>> getEnabledRules() {
        return ApiResponse.success(service.getEnabledRules().stream().map(AiRuleResponse::from).toList());
    }

    @Operation(summary = "규칙 활성/비활성 토글", description = "ruleId로 특정 규칙의 enabled 상태를 변경합니다.")
    @PatchMapping("/{ruleId}/toggle")
    public ApiResponse<AiRuleResponse> toggleRule(
            @PathVariable String ruleId,
            @RequestParam boolean enabled) {
        return ApiResponse.success(AiRuleResponse.from(service.toggleRule(ruleId, enabled)));
    }
}
