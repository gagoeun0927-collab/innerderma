package com.innerderma.skindiagnosis.api;

import com.innerderma.common.response.ApiResponse;
import com.innerderma.skindiagnosis.application.WhsSkinDiagnosisService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users/{userCode}/skin-diagnosis")
public class WhsSkinDiagnosisController {

    private final WhsSkinDiagnosisService diagnosisService;

    public WhsSkinDiagnosisController(WhsSkinDiagnosisService diagnosisService) {
        this.diagnosisService = diagnosisService;
    }

    @GetMapping
    public ApiResponse<WhsSkinDiagnosisResponse> getLatestDiagnosis(@PathVariable String userCode) {
        return ApiResponse.success(
                WhsSkinDiagnosisResponse.from(diagnosisService.getLatestDiagnosis(userCode))
        );
    }
}
