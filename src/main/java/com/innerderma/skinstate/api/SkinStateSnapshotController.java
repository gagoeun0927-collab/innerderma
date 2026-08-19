package com.innerderma.skinstate.api;

import com.innerderma.common.response.ApiResponse;
import com.innerderma.skinstate.application.SkinStateSnapshotService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Skin State Snapshot", description = "피부 상태 스냅샷 — 자가문진 기반 하루 요약")
@RestController
@RequestMapping("/api/users/{userCode}/skin-state-snapshots")
public class SkinStateSnapshotController {

    private final SkinStateSnapshotService snapshotService;

    public SkinStateSnapshotController(SkinStateSnapshotService snapshotService) {
        this.snapshotService = snapshotService;
    }

    @Operation(summary = "스냅샷 생성/갱신", description = "최신 자가문진을 기반으로 오늘의 피부 상태 스냅샷을 생성하거나 갱신합니다.")
    @PostMapping
    public ApiResponse<SkinStateSnapshotResponse> refresh(@PathVariable String userCode) {
        return ApiResponse.success(SkinStateSnapshotResponse.from(
                snapshotService.refreshFromLatestSelfCheck(userCode)));
    }

    @Operation(summary = "최신 스냅샷 조회")
    @GetMapping("/latest")
    public ApiResponse<SkinStateSnapshotResponse> getLatest(@PathVariable String userCode) {
        return ApiResponse.success(SkinStateSnapshotResponse.from(
                snapshotService.getLatest(userCode)));
    }
}
