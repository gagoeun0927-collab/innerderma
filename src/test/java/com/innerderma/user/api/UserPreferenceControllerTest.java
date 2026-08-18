package com.innerderma.user.api;

import com.innerderma.common.error.GlobalExceptionHandler;
import com.innerderma.user.application.UserService;
import com.innerderma.user.domain.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class UserPreferenceControllerTest {

    private UserService userService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        userService = mock(UserService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new UserPreferenceController(userService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void updatesPreferredLocale() throws Exception {
        User user = new User("WHS-DEMO-001", "테스트 사용자", "010-1234-1234");
        user.updatePreferredLocale("ja");
        when(userService.updatePreferredLocale("WHS-DEMO-001", "ja")).thenReturn(user);

        mockMvc.perform(put("/api/users/WHS-DEMO-001/preference")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"locale\":\"ja\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.locale").value("ja"))
                .andExpect(jsonPath("$.data.userCode").value("WHS-DEMO-001"));
    }

    @Test
    void getsPreferredLocale() throws Exception {
        User user = new User("WHS-DEMO-001", "테스트 사용자", "010-1234-1234");
        user.updatePreferredLocale("ko");
        when(userService.getByUserCode("WHS-DEMO-001")).thenReturn(user);

        mockMvc.perform(get("/api/users/WHS-DEMO-001/preference"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.locale").value("ko"));
    }

    @Test
    void rejectsBlankLocale() throws Exception {
        mockMvc.perform(put("/api/users/WHS-DEMO-001/preference")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"locale\":\"\"}"))
                .andExpect(status().isBadRequest());
    }
}
