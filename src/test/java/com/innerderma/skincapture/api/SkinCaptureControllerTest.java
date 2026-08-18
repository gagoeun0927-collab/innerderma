package com.innerderma.skincapture.api;

import com.innerderma.common.error.GlobalExceptionHandler;
import com.innerderma.skincapture.application.SkinCaptureHistoryResult;
import com.innerderma.skincapture.application.SkinCaptureService;
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

class SkinCaptureControllerTest {

    private SkinCaptureService service;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        service = mock(SkinCaptureService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new SkinCaptureController(service))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void returnsCaptureHistory() throws Exception {
        User user = new User("WHS-DEMO-001", "테스트 사용자", "010-1234-1234");
        SkinCapture capture = new SkinCapture(
                user, LocalDate.of(2026, 8, 15), LocalDateTime.of(2026, 8, 15, 12, 0),
                "/images/c.jpg", "face.jpg", "image/jpeg", 3, SkinCaptureQualityStatus.VALID);
        when(service.getHistory("WHS-DEMO-001", LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 17)))
                .thenReturn(new SkinCaptureHistoryResult(
                        LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 17), List.of(capture)));

        mockMvc.perform(get("/api/users/WHS-DEMO-001/skin-captures/history")
                        .param("from", "2026-08-01").param("to", "2026-08-17"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.count").value(1))
                .andExpect(jsonPath("$.data.items[0].originalFilename").value("face.jpg"))
                .andExpect(jsonPath("$.data.items[0].qualityStatus").value("VALID"));
    }

    @Test
    void rejectsInvalidHistoryDate() throws Exception {
        mockMvc.perform(get("/api/users/WHS-DEMO-001/skin-captures/history")
                        .param("from", "2026-99-99"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON_001"));
    }
}
