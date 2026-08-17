package com.innerderma.skindiagnosis.api;

import com.innerderma.common.error.GlobalExceptionHandler;
import com.innerderma.skindiagnosis.application.WhsSkinDiagnosisService;
import com.innerderma.skindiagnosis.domain.*;
import com.innerderma.user.domain.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class WhsSkinDiagnosisControllerTest {
    private WhsSkinDiagnosisService service;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        service = mock(WhsSkinDiagnosisService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new WhsSkinDiagnosisController(service))
                .setControllerAdvice(new GlobalExceptionHandler()).build();
    }

    @Test
    void returns_normalized_metrics_without_inventing_missing_scores() throws Exception {
        WhsSkinDiagnosis diagnosis = new WhsSkinDiagnosis(new User("WHS-1", "사용자", "010"),
                LocalDate.of(2026, 8, 15), "호환용 요약", List.of(
                new WhsSkinDiagnosisMetric(SkinDiagnosisMetricType.BLACKHEAD, null, null,
                        SkinDiagnosisGrade.NEEDS_IMPROVEMENT)));
        when(service.getLatestDiagnosis("WHS-1")).thenReturn(diagnosis);

        mockMvc.perform(get("/api/users/WHS-1/skin-diagnosis"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.resultSummary").value("호환용 요약"))
                .andExpect(jsonPath("$.data.metrics[0].metricType").value("BLACKHEAD"))
                .andExpect(jsonPath("$.data.metrics[0].userScore").doesNotExist())
                .andExpect(jsonPath("$.data.metrics[0].averageScore").doesNotExist())
                .andExpect(jsonPath("$.data.metrics[0].grade").value("NEEDS_IMPROVEMENT"));
    }
}
