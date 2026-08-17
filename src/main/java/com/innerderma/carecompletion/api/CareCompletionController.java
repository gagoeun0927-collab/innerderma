package com.innerderma.carecompletion.api;

import com.innerderma.carecompletion.application.CareCompletionService;
import com.innerderma.common.response.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/users/{userCode}/care-completions")
public class CareCompletionController {
    private final CareCompletionService service;
    public CareCompletionController(CareCompletionService service) { this.service = service; }

    @PutMapping
    public ApiResponse<CareCompletionResponse> save(@PathVariable String userCode,
                                                    @Valid @RequestBody CareCompletionRequest request) {
        return ApiResponse.success(CareCompletionResponse.from(service.save(userCode,
                request.servedDate(), request.phase(), request.completed())));
    }

    @GetMapping
    public ApiResponse<List<CareCompletionResponse>> getDaily(
            @PathVariable String userCode,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ApiResponse.success(service.getDaily(userCode, date).stream()
                .map(CareCompletionResponse::from).toList());
    }
}
