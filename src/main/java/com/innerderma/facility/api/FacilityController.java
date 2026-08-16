package com.innerderma.facility.api;

import com.innerderma.common.response.ApiResponse;
import com.innerderma.facility.application.FacilityService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/facilities")
public class FacilityController {

    private final FacilityService facilityService;

    public FacilityController(FacilityService facilityService) {
        this.facilityService = facilityService;
    }

    @GetMapping
    public ApiResponse<List<FacilityResponse>> getFacilities() {
        List<FacilityResponse> facilities = facilityService.getFacilities().stream()
                .map(FacilityResponse::from)
                .toList();
        return ApiResponse.success(facilities);
    }
}
