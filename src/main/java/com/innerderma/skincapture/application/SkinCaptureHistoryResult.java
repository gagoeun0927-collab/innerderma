package com.innerderma.skincapture.application;

import com.innerderma.skincapture.domain.SkinCapture;

import java.time.LocalDate;
import java.util.List;

public record SkinCaptureHistoryResult(LocalDate from, LocalDate to, List<SkinCapture> items) {}
