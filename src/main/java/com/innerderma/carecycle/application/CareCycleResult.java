package com.innerderma.carecycle.application;

import com.innerderma.carecycle.domain.CareCycle;

import java.time.LocalDate;

public record CareCycleResult(CareCycle careCycle, LocalDate servedDate) {
    public boolean inherited() {
        return servedDate.isAfter(careCycle.getOriginCaptureDate());
    }
}
