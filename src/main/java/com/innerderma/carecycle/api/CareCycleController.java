package com.innerderma.carecycle.api;

import com.innerderma.carecycle.application.CareCycleService;
import com.innerderma.common.response.ApiResponse;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/users/{userCode}/care-cycles")
public class CareCycleController {
    private final CareCycleService careCycleService;

    public CareCycleController(CareCycleService careCycleService) {
        this.careCycleService = careCycleService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<CareCycleResponse> create(@PathVariable String userCode) {
        return ApiResponse.success(CareCycleResponse.from(careCycleService.create(userCode)));
    }

    @GetMapping("/daily")
    public ApiResponse<CareCycleResponse> getDaily(
            @PathVariable String userCode,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ApiResponse.success(CareCycleResponse.from(careCycleService.getDaily(userCode, date)));
    }
}
