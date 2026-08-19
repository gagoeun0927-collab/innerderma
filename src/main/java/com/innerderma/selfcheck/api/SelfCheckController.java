package com.innerderma.selfcheck.api;

import com.innerderma.common.response.ApiResponse;
import com.innerderma.selfcheck.application.SelfCheckService;
import com.innerderma.selfcheck.domain.SelfCheck;
import com.innerderma.skinstate.application.SkinStateSnapshotService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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

@Tag(name = "Self Check", description = "자가문진 — 피부 증상 자가 평가")
@RestController
@RequestMapping("/api/users/{userCode}/self-checks")
public class SelfCheckController {

    private final SelfCheckService selfCheckService;
    private final SkinStateSnapshotService snapshotService;

    public SelfCheckController(SelfCheckService selfCheckService, SkinStateSnapshotService snapshotService) {
        this.selfCheckService = selfCheckService;
        this.snapshotService = snapshotService;
    }

    @Operation(summary = "자가문진 제출", description = "자가문진을 저장하고 피부 상태 스냅샷을 자동 생성/갱신합니다.")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<SelfCheckWithSnapshotResponse> create(
            @PathVariable String userCode,
            @Valid @RequestBody SelfCheckRequest request
    ) {
        SelfCheck selfCheck = selfCheckService.create(userCode, request.toCommand());
        var snapshotResult = snapshotService.refreshFromLatestSelfCheck(userCode);
        return ApiResponse.success(SelfCheckWithSnapshotResponse.from(selfCheck, snapshotResult));
    }

    @Operation(summary = "최신 자가문진 조회")
    @GetMapping("/latest")
    public ApiResponse<SelfCheckResponse> getLatest(@PathVariable String userCode) {
        return ApiResponse.success(SelfCheckResponse.from(selfCheckService.getLatest(userCode)));
    }

    @Operation(summary = "자가문진 이력 조회", description = "기간별 자가문진 기록을 조회합니다.")
    @GetMapping("/history")
    public ApiResponse<SelfCheckHistoryResponse> getHistory(
            @PathVariable String userCode,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ApiResponse.success(SelfCheckHistoryResponse.from(
                selfCheckService.getHistory(userCode, from, to)));
    }
}
