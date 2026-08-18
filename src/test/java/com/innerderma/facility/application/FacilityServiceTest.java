package com.innerderma.facility.application;

import com.innerderma.common.error.BusinessException;
import com.innerderma.common.error.ErrorCode;
import com.innerderma.facility.domain.Facility;
import com.innerderma.facility.domain.FacilityRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class FacilityServiceTest {

    private FacilityRepository facilityRepository;
    private FacilityService service;

    @BeforeEach
    void setUp() {
        facilityRepository = mock(FacilityRepository.class);
        service = new FacilityService(facilityRepository);
    }

    @Test
    void returnsFacilityByCode() {
        when(facilityRepository.findByFacilityCode("WHS"))
                .thenReturn(Optional.of(new Facility("WHS", "웰니스 하우스 서울")));

        Facility result = service.getByFacilityCode("WHS");

        assertThat(result.getName()).isEqualTo("웰니스 하우스 서울");
    }

    @Test
    void throwsFacilityNotFoundWhenCodeDoesNotExist() {
        when(facilityRepository.findByFacilityCode("UNKNOWN")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getByFacilityCode("UNKNOWN"))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.errorCode()).isEqualTo(ErrorCode.FACILITY_NOT_FOUND));
    }
}
