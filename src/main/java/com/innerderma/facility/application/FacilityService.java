package com.innerderma.facility.application;

import com.innerderma.facility.domain.Facility;
import com.innerderma.facility.domain.FacilityRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class FacilityService {

    private final FacilityRepository facilityRepository;

    public FacilityService(FacilityRepository facilityRepository) {
        this.facilityRepository = facilityRepository;
    }

    public List<Facility> getFacilities() {
        return facilityRepository.findAll();
    }
}
