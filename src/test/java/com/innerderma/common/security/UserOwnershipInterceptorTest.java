package com.innerderma.common.security;

import com.innerderma.common.error.BusinessException;
import com.innerderma.common.error.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UserOwnershipInterceptorTest {

    private UserOwnershipInterceptor interceptor;
    private MockHttpServletResponse response;

    @BeforeEach
    void setUp() {
        interceptor = new UserOwnershipInterceptor();
        response = new MockHttpServletResponse();
    }

    @Test
    void allowsWhenHeaderMatchesPathUserCode() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET",
                "/api/users/WHS-DEMO-001/skin-captures/latest");
        request.addHeader(UserOwnershipInterceptor.USER_CODE_HEADER, "WHS-DEMO-001");

        boolean result = interceptor.preHandle(request, response, new Object());

        assertThat(result).isTrue();
    }

    @Test
    void rejectsMissingHeaderWithUnauthorized() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET",
                "/api/users/WHS-DEMO-001/skin-captures/latest");

        assertThatThrownBy(() -> interceptor.preHandle(request, response, new Object()))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.errorCode()).isEqualTo(ErrorCode.UNAUTHORIZED)
                );
    }

    @Test
    void rejectsBlankHeaderWithUnauthorized() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET",
                "/api/users/WHS-DEMO-001");
        request.addHeader(UserOwnershipInterceptor.USER_CODE_HEADER, "   ");

        assertThatThrownBy(() -> interceptor.preHandle(request, response, new Object()))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.errorCode()).isEqualTo(ErrorCode.UNAUTHORIZED)
                );
    }

    @Test
    void rejectsMismatchedHeaderWithForbidden() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET",
                "/api/users/WHS-DEMO-001/self-checks/latest");
        request.addHeader(UserOwnershipInterceptor.USER_CODE_HEADER, "WHS-DEMO-999");

        assertThatThrownBy(() -> interceptor.preHandle(request, response, new Object()))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.errorCode()).isEqualTo(ErrorCode.FORBIDDEN)
                );
    }

    @Test
    void allowsNonUserScopedPathWithoutHeader() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/products/PRD-001");

        boolean result = interceptor.preHandle(request, response, new Object());

        assertThat(result).isTrue();
    }
}
