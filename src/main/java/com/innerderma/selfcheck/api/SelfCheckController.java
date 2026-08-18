package com.innerderma.selfcheck.api;

import com.innerderma.common.response.ApiResponse;
import com.innerderma.selfcheck.application.SelfCheckService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/users/{userCode}/self-checks")
public class SelfCheckController {

    private final SelfCheckService selfCheckService;

    public SelfCheckController(SelfCheckService selfCheckService) {
        this.selfCheckService = selfCheckService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<SelfCheckResponse> create(
            @PathVariable String userCode,
            @Valid @RequestBody SelfCheckRequest request
    ) {
        return ApiResponse.success(SelfCheckResponse.from(selfCheckService.create(userCode, request.toCommand())));
    }

    @GetMapping("/latest")
    public ApiResponse<SelfCheckResponse> getLatest(@PathVariable String userCode) {
        return ApiResponse.success(SelfCheckResponse.from(selfCheckService.getLatest(userCode)));
    }

    @GetMapping("/history")
    public ApiResponse<SelfCheckHistoryResponse> getHistory(
            @PathVariable String userCode,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ApiResponse.success(SelfCheckHistoryResponse.from(
                selfCheckService.getHistory(userCode, from, to)));
    }
}
