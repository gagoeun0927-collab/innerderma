package com.innerderma.common.health;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class HealthController {

    @GetMapping("/api/innerderma/health")
    public Map<String, String> health() {
        return Map.of(
                "status", "ok",
                "message", "InnerDerma server is running"
        );
    }
}

//http://localhost:8080/api/innerderma/health