package com.innerderma.user.api;

import com.innerderma.user.domain.User;

public record UserResponse(
        Long id,
        String userCode,
        String name,
        String phoneNumber
) {
    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getUserCode(),
                user.getName(),
                user.getPhoneNumber()
        );
    }
}
