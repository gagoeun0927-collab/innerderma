package com.innerderma.user.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UserPreferenceRequest(
        @NotBlank @Size(min = 2, max = 10) String locale
) {}
