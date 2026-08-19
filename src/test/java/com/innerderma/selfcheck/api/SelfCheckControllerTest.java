package com.innerderma.selfcheck.api;

import com.innerderma.common.error.GlobalExceptionHandler;
import com.innerderma.selfcheck.application.SelfCheckCommand;
import com.innerderma.selfcheck.application.SelfCheckService;
import com.innerderma.selfcheck.domain.SelfCheck;
import com.innerderma.selfcheck.domain.SymptomSeverity;
import com.innerderma.user.domain.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SelfCheckControllerTest {

    private SelfCheckService service;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        service = mock(SelfCheckService.class);
        var snapshotService = mock(com.innerderma.skinstate.application.SkinStateSnapshotService.class);
        when(snapshotService.refreshFromLatestSelfCheck(any())).thenReturn(
                new com.innerderma.skinstate.application.SkinStateSnapshotResult(
                        new com.innerderma.skinstate.domain.SkinStateSnapshot(
                                new User("WHS-DEMO-001", "test", "010"), java.time.LocalDate.of(2026, 8, 17),
                                "selfcheck-ordinal-v1", "{}", null, "pain", 1L, null,
                                LocalDateTime.of(2026, 8, 17, 12, 30)),
                        java.util.Map.of()));
        mockMvc = MockMvcBuilders.standaloneSetup(new SelfCheckController(service, snapshotService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void createsSelfCheckAndReturnsSafetyFlag() throws Exception {
        User user = new User("WHS-DEMO-001", "테스트 사용자", "010-1234-1234");
        SelfCheck result = new SelfCheck(
                user,
                LocalDateTime.of(2026, 8, 17, 12, 30),
                SymptomSeverity.MODERATE,
                SymptomSeverity.NONE,
                SymptomSeverity.MILD,
                SymptomSeverity.MILD,
                SymptomSeverity.NONE,
                SymptomSeverity.NONE,
                SymptomSeverity.NONE,
                SymptomSeverity.NONE,
                SymptomSeverity.NONE,
                SymptomSeverity.NONE,
                SymptomSeverity.NONE,
                null
        );
        when(service.create(eq("WHS-DEMO-001"), any(SelfCheckCommand.class))).thenReturn(result);

        mockMvc.perform(post("/api/users/WHS-DEMO-001/self-checks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequest("MODERATE")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.selfCheck.pain").value("MODERATE"))
                .andExpect(jsonPath("$.data.selfCheck.requiresSafetyAttention").value(true))
                .andExpect(jsonPath("$.data.snapshot.dominantSymptom").value("pain"));
    }

    @Test
    void rejectsMissingRequiredSymptom() throws Exception {
        mockMvc.perform(post("/api/users/WHS-DEMO-001/self-checks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("COMMON_001"))
                .andExpect(jsonPath("$.errors.pain").exists());
    }

    @Test
    void rejectsUnknownSeverityAsBadRequest() throws Exception {
        mockMvc.perform(post("/api/users/WHS-DEMO-001/self-checks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequest("VERY_HIGH")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON_001"));
    }

    @Test
    void returnsSelfCheckHistory() throws Exception {
        User user = new User("WHS-DEMO-001", "테스트 사용자", "010-1234-1234");
        SelfCheck record = new SelfCheck(
                user, LocalDateTime.of(2026, 8, 17, 12, 30),
                SymptomSeverity.MILD, SymptomSeverity.NONE, SymptomSeverity.NONE, SymptomSeverity.MILD,
                SymptomSeverity.NONE, SymptomSeverity.NONE, SymptomSeverity.NONE, SymptomSeverity.NONE,
                SymptomSeverity.NONE, SymptomSeverity.NONE, SymptomSeverity.NONE, null);
        when(service.getHistory("WHS-DEMO-001", LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 17)))
                .thenReturn(new com.innerderma.selfcheck.application.SelfCheckHistoryResult(
                        LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 17), java.util.List.of(record)));

        mockMvc.perform(get("/api/users/WHS-DEMO-001/self-checks/history")
                        .param("from", "2026-08-01").param("to", "2026-08-17"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.count").value(1))
                .andExpect(jsonPath("$.data.items[0].dryness").value("MILD"));
    }

    @Test
    void rejectsInvalidHistoryDate() throws Exception {
        mockMvc.perform(get("/api/users/WHS-DEMO-001/self-checks/history")
                        .param("from", "2026-99-99"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON_001"));
    }

    private String validRequest(String pain) {
        return """
                {
                  "pain": "%s",
                  "heatSensation": "NONE",
                  "tightness": "MILD",
                  "dryness": "MILD",
                  "itching": "NONE",
                  "swelling": "NONE",
                  "peeling": "NONE",
                  "breakout": "NONE",
                  "oozing": "NONE",
                  "bleeding": "NONE",
                  "barrierDamage": "NONE"
                }
                """.formatted(pain);
    }
}
