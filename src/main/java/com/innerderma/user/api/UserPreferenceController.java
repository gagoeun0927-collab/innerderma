package com.innerderma.user.api;

import com.innerderma.common.response.ApiResponse;
import com.innerderma.user.application.UserService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users/{userCode}/preference")
public class UserPreferenceController {

    private final UserService userService;

    public UserPreferenceController(UserService userService) {
        this.userService = userService;
    }

    @PutMapping
    public ApiResponse<UserPreferenceResponse> updatePreference(
            @PathVariable String userCode,
            @Valid @RequestBody UserPreferenceRequest request) {
        return ApiResponse.success(UserPreferenceResponse.from(
                userService.updatePreferredLocale(userCode, request.locale())));
    }

    @GetMapping
    public ApiResponse<UserPreferenceResponse> getPreference(@PathVariable String userCode) {
        return ApiResponse.success(UserPreferenceResponse.from(
                userService.getByUserCode(userCode)));
    }
}
