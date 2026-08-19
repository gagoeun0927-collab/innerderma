package com.innerderma.common.security;

import com.innerderma.common.response.ApiResponse;
import com.innerderma.user.application.UserService;
import com.innerderma.user.domain.User;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 대회 시연용 인증 API. 회원가입 + 토큰 발급.
 * 비밀번호 없이 userCode 기반으로 동작한다.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final JwtProvider jwtProvider;
    private final UserService userService;

    public AuthController(JwtProvider jwtProvider, UserService userService) {
        this.jwtProvider = jwtProvider;
        this.userService = userService;
    }

    @PostMapping("/register")
    public ApiResponse<RegisterResponse> register(@Valid @RequestBody RegisterRequest request) {
        User user = userService.register(request.userCode(), request.name(), request.phoneNumber());
        String token = jwtProvider.generateToken(user.getUserCode());
        return ApiResponse.success(new RegisterResponse(token, user.getUserCode(), user.getName()));
    }

    @PostMapping("/token")
    public ApiResponse<TokenResponse> issueToken(@RequestParam String userCode) {
        userService.getByUserCode(userCode);
        String token = jwtProvider.generateToken(userCode);
        return ApiResponse.success(new TokenResponse(token, userCode));
    }

    public record RegisterRequest(
            @NotBlank String userCode,
            @NotBlank String name,
            @NotBlank String phoneNumber
    ) {}

    public record RegisterResponse(String token, String userCode, String name) {}
    public record TokenResponse(String token, String userCode) {}
}
