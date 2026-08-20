package com.innerderma.caresolution.api;

import com.innerderma.carecycle.domain.CareCycle;
import com.innerderma.caresolution.application.CareSolutionResult;
import com.innerderma.caresolution.application.CareSolutionService;
import com.innerderma.caresolution.domain.*;
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
import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CareSolutionControllerTest {
    private CareSolutionService service;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        service = mock(CareSolutionService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new CareSolutionController(service))
                .setControllerAdvice(new GlobalExceptionHandler()).build();
    }

    @Test
    void returnsDailyRoutineAndSafetyPriority() throws Exception {
        CareSolution solution = solution();
        when(service.getDaily("WHS-DEMO-001", LocalDate.of(2026, 8, 18)))
                .thenReturn(new CareSolutionResult(solution, List.of("저녁 최소 보습"),
                        List.of("아침 자외선 차단"), LocalDate.of(2026, 8, 18)));

        mockMvc.perform(get("/api/users/WHS-DEMO-001/care-solutions/daily")
                        .param("date", "2026-08-18"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.safetyLevel").value("ATTENTION"))
                .andExpect(jsonPath("$.data.inherited").value(true))
                .andExpect(jsonPath("$.data.generationType").value("CARRIED_FORWARD"))
                .andExpect(jsonPath("$.data.eveningSteps[0].description").value("저녁 최소 보습"))
                .andExpect(jsonPath("$.data.morningSteps[0].description").value("아침 자외선 차단"))
                .andExpect(jsonPath("$.data.eveningAvoid").isArray())
                .andExpect(jsonPath("$.data.supplements").isArray())
                .andExpect(jsonPath("$.data.concernTags").isArray())
                .andExpect(jsonPath("$.data.eveningWash.title").value("저녁 세안 루틴"));
    }

    @Test
    void rejectsInvalidDate() throws Exception {
        mockMvc.perform(get("/api/users/WHS-DEMO-001/care-solutions/daily")
                        .param("date", "2026-99-99"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON_001"));
    }

    private CareSolution solution() {
        User user = new User("WHS-DEMO-001", "테스트 사용자", "010-1234-1234");
        SkinCapture capture = new SkinCapture(user, LocalDate.of(2026, 8, 17),
                LocalDateTime.of(2026, 8, 17, 10, 0), "/images/face.jpg", "face.jpg",
                "image/jpeg", 3, SkinCaptureQualityStatus.VALID);
        SkinAnalysis analysis = new SkinAnalysis(capture, LocalDateTime.of(2026, 8, 17, 10, 1),
                70, "Good", "1.0", "{}");
        CareCycle cycle = new CareCycle(user, analysis, null, LocalDate.of(2026, 8, 17),
                LocalDateTime.of(2026, 8, 17, 10, 2));
        return new CareSolution(cycle, null, null, CareSeason.SUMMER, SafetyLevel.ATTENTION,
                "안전 우선", "[]", "[]", "의료진에게 문의하세요.", "redness",
                LocalDateTime.of(2026, 8, 17, 10, 3));
    }
}
