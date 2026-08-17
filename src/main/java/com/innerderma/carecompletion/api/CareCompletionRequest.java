package com.innerderma.carecompletion.api;

import com.innerderma.carehistory.application.CarePhase;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record CareCompletionRequest(LocalDate servedDate, @NotNull CarePhase phase,
                                    @NotNull Boolean completed) {}
