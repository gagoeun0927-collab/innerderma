package com.innerderma.skincapture.infrastructure;

import com.innerderma.common.error.BusinessException;
import com.innerderma.common.error.ErrorCode;
import com.innerderma.skincapture.application.SkinCaptureFile;
import com.innerderma.skincapture.application.SkinCaptureStorage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Map;
import java.util.UUID;

@Component
public class LocalSkinCaptureStorage implements SkinCaptureStorage {

    private static final Map<String, String> EXTENSIONS = Map.of(
            "image/jpeg", ".jpg",
            "image/png", ".png",
            "image/webp", ".webp"
    );

    private final Path root;

    public LocalSkinCaptureStorage(@Value("${innerderma.skin-capture.storage-path:./data/skin-captures}") String root) {
        this.root = Path.of(root).toAbsolutePath().normalize();
    }

    @Override
    public String store(SkinCaptureFile file) {
        String extension = EXTENSIONS.get(file.contentType());
        if (extension == null) {
            throw new BusinessException(ErrorCode.INVALID_SKIN_CAPTURE_IMAGE);
        }

        try {
            Files.createDirectories(root);
            Path destination = root.resolve(UUID.randomUUID() + extension).normalize();
            if (!destination.startsWith(root)) {
                throw new BusinessException(ErrorCode.INVALID_SKIN_CAPTURE_IMAGE);
            }
            Files.write(destination, file.bytes(), StandardOpenOption.CREATE_NEW);
            return destination.toString();
        } catch (IOException exception) {
            throw new BusinessException(ErrorCode.SKIN_CAPTURE_STORAGE_FAILED);
        }
    }
}
