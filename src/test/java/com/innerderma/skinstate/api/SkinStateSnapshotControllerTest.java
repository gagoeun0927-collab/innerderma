package com.innerderma.skinstate.api;

import com.innerderma.common.error.BusinessException;
import com.innerderma.common.error.ErrorCode;
import com.innerderma.common.error.GlobalExceptionHandler;
import com.innerderma.skinstate.application.SkinStateSnapshotResult;
import com.innerderma.skinstate.domain.SkinStateSnapshot;
import com.innerderma.user.domain.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SkinStateSnapshotControllerTest {

    private com.innerderma.skinstate.application.SkinStateSnapshotService service;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        service = mock(com.innerderma.skinstate.application.SkinStateSnapshotService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new SkinStateSnapshotController(service))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private SkinStateSnapshotResult sampleResult() {
        SkinStateSnapshot snapshot = new SkinStateSnapshot(
                new User("WHS-DEMO-001", "테스트 사용자", "010-1234-1234"),
                LocalDate.of(2026, 8, 17), "selfcheck-ordinal-v1",
                "{\"dryness\":3}", null, "dryness", 5L, null, LocalDateTime.of(2026, 8, 17, 12, 30));
        return new SkinStateSnapshotResult(snapshot, Map.of("dryness", 3));
    }

    @Test
    void refreshReturnsSnapshot() throws Exception {
        when(service.refreshFromLatestSelfCheck("WHS-DEMO-001")).thenReturn(sampleResult());

        mockMvc.perform(post("/api/users/WHS-DEMO-001/skin-state-snapshots"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.scoringVersion").value("selfcheck-ordinal-v1"))
                .andExpect(jsonPath("$.data.dominantSymptom").value("dryness"))
                .andExpect(jsonPath("$.data.symptomScores.dryness").value(3));
    }

    @Test
    void getLatestReturnsSnapshot() throws Exception {
        when(service.getLatest("WHS-DEMO-001")).thenReturn(sampleResult());

        mockMvc.perform(get("/api/users/WHS-DEMO-001/skin-state-snapshots/latest"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.sourceSelfCheckId").value(5));
    }

    @Test
    void getLatestReturnsNotFoundWhenNoSnapshot() throws Exception {
        when(service.getLatest("WHS-DEMO-001"))
                .thenThrow(new BusinessException(ErrorCode.SKIN_STATE_SNAPSHOT_NOT_FOUND));

        mockMvc.perform(get("/api/users/WHS-DEMO-001/skin-state-snapshots/latest"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("SNAPSHOT_001"));
    }
}
