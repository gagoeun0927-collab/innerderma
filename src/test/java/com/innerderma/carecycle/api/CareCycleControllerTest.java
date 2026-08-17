package com.innerderma.carecycle.api;

import com.innerderma.carecycle.application.CareCycleResult;
import com.innerderma.carecycle.application.CareCycleService;
import com.innerderma.carecycle.domain.CareCycle;
import com.innerderma.common.error.GlobalExceptionHandler;
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

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CareCycleControllerTest {
    private CareCycleService service;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        service = mock(CareCycleService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new CareCycleController(service))
                .setControllerAdvice(new GlobalExceptionHandler()).build();
    }

    @Test
    void returnsInheritedCycleForRequestedDate() throws Exception {
        User user = new User("WHS-DEMO-001", "테스트 사용자", "010-1234-1234");
        SkinCapture capture = new SkinCapture(user, LocalDate.of(2026, 8, 17),
                LocalDateTime.of(2026, 8, 17, 10, 0), "/images/face.jpg", "face.jpg",
                "image/jpeg", 3, SkinCaptureQualityStatus.VALID);
        SkinAnalysis analysis = new SkinAnalysis(capture, LocalDateTime.of(2026, 8, 17, 10, 1),
                80, "Good", "1.0", "{}");
        CareCycle cycle = new CareCycle(user, analysis, null, LocalDate.of(2026, 8, 17),
                LocalDateTime.of(2026, 8, 17, 12, 30));
        when(service.getDaily("WHS-DEMO-001", LocalDate.of(2026, 8, 19)))
                .thenReturn(new CareCycleResult(cycle, LocalDate.of(2026, 8, 19)));

        mockMvc.perform(get("/api/users/WHS-DEMO-001/care-cycles/daily")
                        .param("date", "2026-08-19"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.originCaptureDate").value("2026-08-17"))
                .andExpect(jsonPath("$.data.morningCareDate").value("2026-08-18"))
                .andExpect(jsonPath("$.data.servedDate").value("2026-08-19"))
                .andExpect(jsonPath("$.data.inherited").value(true));
    }

    @Test
    void rejectsInvalidDateAsBadRequest() throws Exception {
        mockMvc.perform(get("/api/users/WHS-DEMO-001/care-cycles/daily")
                        .param("date", "not-a-date"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON_001"));
    }
}
