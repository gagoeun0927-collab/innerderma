package com.innerderma.carecompletion.api;

import com.innerderma.carecompletion.domain.CareCompletion;
import com.innerderma.carehistory.application.CarePhase;

import java.time.*;

public record CareCompletionResponse(Long id, LocalDate servedDate, CarePhase phase,
                                     boolean completed, Long careSolutionId,
                                     LocalDate originCaptureDate, LocalDateTime updatedAt) {
    public static CareCompletionResponse from(CareCompletion completion) {
        return new CareCompletionResponse(completion.getId(), completion.getServedDate(),
                completion.getPhase(), completion.isCompleted(), completion.getCareSolution().getId(),
                completion.getCareSolution().getCareCycle().getOriginCaptureDate(), completion.getUpdatedAt());
    }
}
