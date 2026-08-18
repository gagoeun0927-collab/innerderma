package com.innerderma.user.api;

import com.innerderma.user.domain.User;

public record UserPreferenceResponse(String userCode, String locale) {
    public static UserPreferenceResponse from(User user) {
        return new UserPreferenceResponse(user.getUserCode(), user.getPreferredLocale());
    }
}
