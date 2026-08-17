package com.innerderma.skincapture.infrastructure;

import com.innerderma.common.error.BusinessException;
import com.innerderma.common.error.ErrorCode;
import com.innerderma.skincapture.application.SkinCaptureFile;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LocalSkinCaptureStorageTest {

    @TempDir
    Path tempDir;

    @Test
    void storesImageUnderConfiguredDirectoryWithServerGeneratedName() throws Exception {
        LocalSkinCaptureStorage storage = new LocalSkinCaptureStorage(tempDir.toString());
        byte[] bytes = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0};

        String storedPath = storage.store(new SkinCaptureFile("../../face.jpg", "image/jpeg", bytes.length, bytes));

        Path result = Path.of(storedPath);
        assertThat(result).startsWith(tempDir);
        assertThat(result.getFileName().toString()).endsWith(".jpg").doesNotContain("face");
        assertThat(Files.readAllBytes(result)).isEqualTo(bytes);
    }

    @Test
    void rejectsContentTypeThatHasNoSafeExtension() {
        LocalSkinCaptureStorage storage = new LocalSkinCaptureStorage(tempDir.toString());
        SkinCaptureFile file = new SkinCaptureFile("face.svg", "image/svg+xml", 1, new byte[]{1});

        assertThatThrownBy(() -> storage.store(file))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.errorCode()).isEqualTo(ErrorCode.INVALID_SKIN_CAPTURE_IMAGE)
                );
    }

    @Test
    void loadsPreviouslyStoredImage() {
        LocalSkinCaptureStorage storage = new LocalSkinCaptureStorage(tempDir.toString());
        byte[] bytes = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0};
        String storedPath = storage.store(new SkinCaptureFile("face.jpg", "image/jpeg", bytes.length, bytes));

        assertThat(storage.load(storedPath)).isEqualTo(bytes);
    }

    @Test
    void refusesToLoadFileOutsideConfiguredDirectory() throws Exception {
        LocalSkinCaptureStorage storage = new LocalSkinCaptureStorage(tempDir.resolve("safe").toString());
        Path outside = tempDir.resolve("outside.jpg");
        Files.write(outside, new byte[]{1});

        assertThatThrownBy(() -> storage.load(outside.toString()))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.errorCode()).isEqualTo(ErrorCode.SKIN_ANALYSIS_IMAGE_NOT_AVAILABLE)
                );
    }
}
