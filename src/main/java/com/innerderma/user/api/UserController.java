package com.innerderma.user.api;

import com.innerderma.common.response.ApiResponse;
import com.innerderma.user.application.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "User", description = "사용자 프로필 조회/수정")
@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @Operation(summary = "사용자 조회", description = "userCode로 사용자 프로필을 조회합니다.")
    @GetMapping("/{userCode}")
    public ApiResponse<UserResponse> getUser(@PathVariable String userCode) {
        return ApiResponse.success(UserResponse.from(userService.getByUserCode(userCode)));
    }

    @Operation(summary = "프로필 수정", description = "이름, 전화번호를 변경합니다.")
    @PutMapping("/{userCode}")
    public ApiResponse<UserResponse> updateProfile(@PathVariable String userCode,
                                                   @RequestBody UpdateProfileRequest request) {
        return ApiResponse.success(UserResponse.from(
                userService.updateProfile(userCode, request.name(), request.phoneNumber())));
    }

    public record UpdateProfileRequest(String name, String phoneNumber) {}
}
