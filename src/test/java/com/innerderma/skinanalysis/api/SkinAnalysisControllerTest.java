package com.innerderma.skinanalysis.api;

import com.innerderma.common.error.GlobalExceptionHandler;
import com.innerderma.skinanalysis.application.SkinAnalysisHistoryResult;
import com.innerderma.skinanalysis.application.SkinAnalysisService;
import com.innerderma.skinanalysis.domain.SkinAnalysis;
import com.innerderma.skincapture.domain.SkinCapture;
import com.innerderma.skincapture.domain.SkinCaptureQualityStatus;
import com.innerderma.user.domain.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SkinAnalysisControllerTest {

    private SkinAnalysisService service;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        service = mock(SkinAnalysisService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new SkinAnalysisController(service))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void returnsAnalysisHistorySummaries() throws Exception {
        User user = new User("WHS-DEMO-001", "테스트 사용자", "010-1234-1234");
        SkinCapture capture = new SkinCapture(
                user, LocalDate.of(2026, 8, 17), LocalDateTime.of(2026, 8, 17, 12, 0),
                "/images/face.jpg", "face.jpg", "image/jpeg", 3, SkinCaptureQualityStatus.VALID);
        SkinAnalysis analysis = new SkinAnalysis(
                capture, LocalDateTime.of(2026, 8, 17, 12, 30), 78.5, "Good", "1.0.0", "{}");
        when(service.getHistory("WHS-DEMO-001", LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 17)))
                .thenReturn(new SkinAnalysisHistoryResult(
                        LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 17), List.of(analysis)));

        mockMvc.perform(get("/api/users/WHS-DEMO-001/skin-analyses/history")
                        .param("from", "2026-08-01").param("to", "2026-08-17"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.count").value(1))
                .andExpect(jsonPath("$.data.items[0].overallScore").value(78.5))
                .andExpect(jsonPath("$.data.items[0].skinHealthGrade").value("Good"));
    }

    @Test
    void rejectsInvalidHistoryDate() throws Exception {
        mockMvc.perform(get("/api/users/WHS-DEMO-001/skin-analyses/history")
                        .param("from", "2026-99-99"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON_001"));
    }
}
