package com.innerderma.skincapture.application;

public record SkinCaptureFile(
        String originalFilename,
        String contentType,
        long size,
        byte[] bytes
) {
}
