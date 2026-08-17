package com.innerderma.carehistory.api;

import com.innerderma.carehistory.application.CareHistoryService;
import com.innerderma.common.response.ApiResponse;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/users/{userCode}/care-history")
public class CareHistoryController {
    private final CareHistoryService service;

    public CareHistoryController(CareHistoryService service) {
        this.service = service;
    }

    @GetMapping
    public ApiResponse<CareHistoryResponse> getHistory(
            @PathVariable String userCode,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ApiResponse.success(CareHistoryResponse.from(service.getHistory(userCode, from, to)));
    }
}
