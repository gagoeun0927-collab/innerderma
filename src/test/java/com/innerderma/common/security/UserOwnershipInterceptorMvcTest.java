package com.innerderma.common.security;

import com.innerderma.common.error.GlobalExceptionHandler;
import com.innerderma.user.api.UserController;
import com.innerderma.user.application.UserService;
import com.innerderma.user.domain.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class UserOwnershipInterceptorMvcTest {

    private UserService userService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        userService = mock(UserService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new UserController(userService))
                .addMappedInterceptors(new String[]{"/api/users/**"}, new UserOwnershipInterceptor())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void returnsUnauthorizedWhenHeaderMissing() throws Exception {
        mockMvc.perform(get("/api/users/WHS-DEMO-001"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_001"));
    }

    @Test
    void returnsForbiddenWhenHeaderMismatched() throws Exception {
        mockMvc.perform(get("/api/users/WHS-DEMO-001")
                        .header(UserOwnershipInterceptor.USER_CODE_HEADER, "WHS-DEMO-999"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("AUTH_002"));
    }

    @Test
    void allowsWhenHeaderMatches() throws Exception {
        when(userService.getByUserCode("WHS-DEMO-001"))
                .thenReturn(new User("WHS-DEMO-001", "테스트 사용자", "010-1234-1234"));

        mockMvc.perform(get("/api/users/WHS-DEMO-001")
                        .header(UserOwnershipInterceptor.USER_CODE_HEADER, "WHS-DEMO-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.userCode").value("WHS-DEMO-001"));
    }
}
