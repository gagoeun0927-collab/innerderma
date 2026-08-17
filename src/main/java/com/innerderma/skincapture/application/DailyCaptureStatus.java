package com.innerderma.skincapture.application;

import com.innerderma.skincapture.domain.SkinCapture;
import java.time.LocalDate;

public record DailyCaptureStatus(LocalDate date, boolean canCapture, SkinCapture capture) {}
