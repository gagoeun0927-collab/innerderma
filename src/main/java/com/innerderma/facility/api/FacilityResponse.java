package com.innerderma.facility.api;

import com.innerderma.facility.domain.Facility;

public record FacilityResponse(
        Long id,
        String facilityCode,
        String name
) {
    public static FacilityResponse from(Facility facility) {
        return new FacilityResponse(
                facility.getId(),
                facility.getFacilityCode(),
                facility.getName()
        );
    }
}
