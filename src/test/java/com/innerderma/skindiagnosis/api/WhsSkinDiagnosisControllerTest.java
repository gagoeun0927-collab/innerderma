package com.innerderma.skindiagnosis.api;

import com.innerderma.common.error.GlobalExceptionHandler;
import com.innerderma.skindiagnosis.application.WhsSkinDiagnosisHistoryResult;
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
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
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

    @Test
    void returnsDiagnosisHistory() throws Exception {
        User user = new User("WHS-DEMO-001", "테스트 사용자", "010-1234-1234");
        WhsSkinDiagnosis diagnosis = new WhsSkinDiagnosis(user, LocalDate.of(2026, 8, 10), "건성 경향");
        when(service.getHistory("WHS-DEMO-001", LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 17)))
                .thenReturn(new WhsSkinDiagnosisHistoryResult(
                        LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 17), List.of(diagnosis)));

        mockMvc.perform(get("/api/users/WHS-DEMO-001/skin-diagnosis/history")
                        .param("from", "2026-08-01").param("to", "2026-08-17"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.count").value(1))
                .andExpect(jsonPath("$.data.items[0].resultSummary").value("건성 경향"));
    }

    @Test
    void rejectsInvalidHistoryDate() throws Exception {
        mockMvc.perform(get("/api/users/WHS-DEMO-001/skin-diagnosis/history")
                        .param("from", "2026-99-99"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON_001"));
    }
}
