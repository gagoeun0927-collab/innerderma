package com.innerderma.dailycare.api;

import com.innerderma.common.response.ApiResponse;
import com.innerderma.dailycare.application.DailyCareService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/users/{userCode}/daily-care")
public class DailyCareController {
    private final DailyCareService service;

    public DailyCareController(DailyCareService service) {
        this.service = service;
    }

    @GetMapping
    public ApiResponse<DailyCareResponse> getDaily(
            @PathVariable String userCode,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ApiResponse.success(DailyCareResponse.from(service.getDaily(userCode, date)));
    }
}
