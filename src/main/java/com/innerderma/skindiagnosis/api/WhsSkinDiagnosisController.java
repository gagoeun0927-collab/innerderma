package com.innerderma.skindiagnosis.api;

import com.innerderma.common.response.ApiResponse;
import com.innerderma.skindiagnosis.application.WhsSkinDiagnosisService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

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

    @GetMapping("/history")
    public ApiResponse<WhsSkinDiagnosisHistoryResponse> getHistory(
            @PathVariable String userCode,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ApiResponse.success(WhsSkinDiagnosisHistoryResponse.from(
                diagnosisService.getHistory(userCode, from, to)));
    }
}
