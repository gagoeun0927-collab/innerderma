package com.innerderma.user.api;

import com.innerderma.common.response.ApiResponse;
import com.innerderma.user.application.UserService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/{userCode}")
    public ApiResponse<UserResponse> getUser(@PathVariable String userCode) {
        return ApiResponse.success(UserResponse.from(userService.getByUserCode(userCode)));
    }
}
