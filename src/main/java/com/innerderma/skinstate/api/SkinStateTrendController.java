package com.innerderma.skinstate.api;

import com.innerderma.common.response.ApiResponse;
import com.innerderma.skinstate.trend.SkinStateTrendService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users/{userCode}/skin-state-trend")
public class SkinStateTrendController {

    private final SkinStateTrendService trendService;

    public SkinStateTrendController(SkinStateTrendService trendService) {
        this.trendService = trendService;
    }

    @GetMapping
    public ApiResponse<SkinStateTrendResponse> getLatestTrend(@PathVariable String userCode) {
        return ApiResponse.success(SkinStateTrendResponse.from(trendService.evaluateLatest(userCode)));
    }
}
