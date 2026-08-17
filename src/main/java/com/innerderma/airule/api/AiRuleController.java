package com.innerderma.airule.api;

import com.innerderma.airule.application.AiRuleService;
import com.innerderma.common.response.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/ai-rules")
public class AiRuleController {
    private final AiRuleService service;

    public AiRuleController(AiRuleService service) {
        this.service = service;
    }

    @GetMapping
    public ApiResponse<List<AiRuleResponse>> getEnabledRules() {
        return ApiResponse.success(service.getEnabledRules().stream().map(AiRuleResponse::from).toList());
    }
}
