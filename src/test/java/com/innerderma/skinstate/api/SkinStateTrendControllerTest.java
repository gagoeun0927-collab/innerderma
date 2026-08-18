package com.innerderma.skinstate.api;

import com.innerderma.common.error.BusinessException;
import com.innerderma.common.error.ErrorCode;
import com.innerderma.common.error.GlobalExceptionHandler;
import com.innerderma.skinstate.trend.SkinStateTrend;
import com.innerderma.skinstate.trend.SkinStateTrendService;
import com.innerderma.skinstate.trend.TrendResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
import java.util.Map;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SkinStateTrendControllerTest {

    private SkinStateTrendService service;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        service = mock(SkinStateTrendService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new SkinStateTrendController(service))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void returnsTrendWithRuleSignals() throws Exception {
        TrendResult result = new TrendResult(SkinStateTrend.WORSENING,
                Map.of("dryness", SkinStateTrend.WORSENING),
                "selfcheck-ordinal-v1", LocalDate.of(2026, 8, 17), LocalDate.of(2026, 8, 16));
        when(service.evaluateLatest("WHS-DEMO-001")).thenReturn(result);

        mockMvc.perform(get("/api/users/WHS-DEMO-001/skin-state-trend"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.overallTrend").value("WORSENING"))
                .andExpect(jsonPath("$.data.symptomTrends.dryness").value("WORSENING"))
                .andExpect(jsonPath("$.data.ruleSignals.trend_worsening").value(true));
    }

    @Test
    void returnsUserNotFound() throws Exception {
        when(service.evaluateLatest("WHS-DEMO-001"))
                .thenThrow(new BusinessException(ErrorCode.USER_NOT_FOUND));

        mockMvc.perform(get("/api/users/WHS-DEMO-001/skin-state-trend"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("USER_001"));
    }
}
