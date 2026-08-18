package com.innerderma.facility.api;

import com.innerderma.common.error.BusinessException;
import com.innerderma.common.error.ErrorCode;
import com.innerderma.common.error.GlobalExceptionHandler;
import com.innerderma.facility.application.FacilityService;
import com.innerderma.facility.domain.Facility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class FacilityControllerTest {

    private FacilityService service;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        service = mock(FacilityService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new FacilityController(service))
                .setControllerAdvice(new GlobalExceptionHandler()).build();
    }

    @Test
    void returnsFacilityByCode() throws Exception {
        when(service.getByFacilityCode("WHS")).thenReturn(new Facility("WHS", "웰니스 하우스 서울"));

        mockMvc.perform(get("/api/facilities/{facilityCode}", "WHS"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.facilityCode").value("WHS"))
                .andExpect(jsonPath("$.data.name").value("웰니스 하우스 서울"));
    }

    @Test
    void returnsNotFoundForUnknownCode() throws Exception {
        when(service.getByFacilityCode("UNKNOWN"))
                .thenThrow(new BusinessException(ErrorCode.FACILITY_NOT_FOUND));

        mockMvc.perform(get("/api/facilities/{facilityCode}", "UNKNOWN"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("FACILITY_001"));
    }
}
