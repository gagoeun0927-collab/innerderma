package com.innerderma.common.security;

import com.innerderma.common.response.ApiResponse;
import com.innerderma.user.application.UserService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 대회 시연용 토큰 발급 API. 회원가입/비밀번호 없이 userCode만으로 JWT 발급.
 * 실제 운영에서는 인증 절차를 추가해야 한다.
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

    @PostMapping("/token")
    public ApiResponse<TokenResponse> issueToken(@RequestParam String userCode) {
        // 사용자 존재 확인
        userService.getByUserCode(userCode);
        String token = jwtProvider.generateToken(userCode);
        return ApiResponse.success(new TokenResponse(token, userCode));
    }

    public record TokenResponse(String token, String userCode) {}
}
