package com.innerderma.procedure.api;

import com.innerderma.common.error.GlobalExceptionHandler;
import com.innerderma.procedure.application.ProcedureRecordService;
import com.innerderma.procedure.application.TreatmentContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ProcedureRecordControllerTest {
    private ProcedureRecordService service;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        service = mock(ProcedureRecordService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new ProcedureRecordController(service))
                .setControllerAdvice(new GlobalExceptionHandler()).build();
    }

    @Test
    void returnsTreatmentContextForSpecifiedDate() throws Exception {
        LocalDate date = LocalDate.of(2026, 8, 18);
        when(service.getTreatmentContext("WHS-DEMO-001", date)).thenReturn(new TreatmentContext(
                1L, null, null, LocalDate.of(2026, 8, 15), null, 3,
                null, null, List.of(), List.of(), List.of("기존 관리 가이드"),
                List.of(), List.of(), null, null));

        mockMvc.perform(get("/api/users/WHS-DEMO-001/procedures/treatment-context")
                        .param("date", "2026-08-18"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.treatmentDate").value("2026-08-15"))
                .andExpect(jsonPath("$.data.daysSinceTreatment").value(3))
                .andExpect(jsonPath("$.data.aftercareRestrictions").isArray());
    }

    @Test
    void rejectsInvalidReferenceDate() throws Exception {
        mockMvc.perform(get("/api/users/WHS-DEMO-001/procedures/treatment-context")
                        .param("date", "future-ish"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON_001"));
    }
}
