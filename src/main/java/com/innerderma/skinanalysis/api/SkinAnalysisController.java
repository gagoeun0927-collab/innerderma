package com.innerderma.skinanalysis.api;

import com.innerderma.common.response.ApiResponse;
import com.innerderma.skinanalysis.application.SkinAnalysisService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users/{userCode}/skin-analyses")
public class SkinAnalysisController {

    private final SkinAnalysisService skinAnalysisService;

    public SkinAnalysisController(SkinAnalysisService skinAnalysisService) {
        this.skinAnalysisService = skinAnalysisService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<SkinAnalysisResponse> analyze(
            @PathVariable String userCode,
            @Valid @RequestBody SkinAnalysisRequest request
    ) {
        return ApiResponse.success(SkinAnalysisResponse.from(
                skinAnalysisService.analyze(userCode, request.captureId(), request.actualAge())
        ));
    }

    @GetMapping("/latest")
    public ApiResponse<SkinAnalysisResponse> getLatest(@PathVariable String userCode) {
        return ApiResponse.success(SkinAnalysisResponse.from(skinAnalysisService.getLatest(userCode)));
    }
}
