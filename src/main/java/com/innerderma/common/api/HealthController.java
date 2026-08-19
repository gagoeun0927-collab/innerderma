package com.innerderma.common.api;

import com.innerderma.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Map;

@Tag(name = "System", description = "시스템 상태 확인")
@RestController
@RequestMapping("/api/innerderma")
public class HealthController {

    @Operation(summary = "서버 상태 확인", description = "서버가 정상 동작 중인지 확인합니다.")
    @GetMapping("/health")
    public ApiResponse<Map<String, Object>> health() {
        return ApiResponse.success(Map.of(
                "status", "UP",
                "service", "InnerDerma Backend",
                "timestamp", LocalDateTime.now().toString()
        ));
    }
}
