package com.innerderma.skinstate.api;

import com.innerderma.common.response.ApiResponse;
import com.innerderma.skinstate.application.SkinStateSnapshotService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users/{userCode}/skin-state-snapshots")
public class SkinStateSnapshotController {

    private final SkinStateSnapshotService snapshotService;

    public SkinStateSnapshotController(SkinStateSnapshotService snapshotService) {
        this.snapshotService = snapshotService;
    }

    @PostMapping
    public ApiResponse<SkinStateSnapshotResponse> refresh(@PathVariable String userCode) {
        return ApiResponse.success(SkinStateSnapshotResponse.from(
                snapshotService.refreshFromLatestSelfCheck(userCode)));
    }

    @GetMapping("/latest")
    public ApiResponse<SkinStateSnapshotResponse> getLatest(@PathVariable String userCode) {
        return ApiResponse.success(SkinStateSnapshotResponse.from(
                snapshotService.getLatest(userCode)));
    }
}
