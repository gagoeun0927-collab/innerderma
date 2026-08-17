package com.innerderma.caresolution.api;

import com.innerderma.caresolution.application.CareSolutionService;
import com.innerderma.common.response.ApiResponse;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/users/{userCode}/care-solutions")
public class CareSolutionController {
    private final CareSolutionService service;

    public CareSolutionController(CareSolutionService service) { this.service = service; }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<CareSolutionResponse> create(
            @PathVariable String userCode,
            @RequestBody(required = false) CareSolutionRequest request) {
        Long cycleId = request == null ? null : request.careCycleId();
        return ApiResponse.success(CareSolutionResponse.from(service.create(userCode, cycleId)));
    }

    @GetMapping("/daily")
    public ApiResponse<CareSolutionResponse> getDaily(
            @PathVariable String userCode,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ApiResponse.success(CareSolutionResponse.from(service.getDaily(userCode, date)));
    }
}
