package com.innerderma.skincapture.application;

import com.innerderma.common.error.BusinessException;
import com.innerderma.common.error.ErrorCode;
import com.innerderma.skincapture.domain.SkinCapture;
import com.innerderma.skincapture.domain.SkinCaptureQualityStatus;
import com.innerderma.skincapture.domain.SkinCaptureRepository;
import com.innerderma.user.domain.User;
import com.innerderma.user.domain.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SkinCaptureServiceTest {

    private static final String USER_CODE = "WHS-DEMO-001";
    private static final Clock CLOCK = Clock.fixed(
            Instant.parse("2026-08-17T03:30:00Z"),
            ZoneId.of("Asia/Seoul")
    );

    private SkinCaptureRepository skinCaptureRepository;
    private UserRepository userRepository;
    private SkinCaptureStorage storage;
    private SkinCaptureService service;

    @BeforeEach
    void setUp() {
        skinCaptureRepository = mock(SkinCaptureRepository.class);
        userRepository = mock(UserRepository.class);
        storage = mock(SkinCaptureStorage.class);
        service = new SkinCaptureService(skinCaptureRepository, userRepository, storage, CLOCK);
    }

    @Test
    void createsOneValidCaptureUsingKoreanDate() {
        User user = new User(USER_CODE, "테스트 사용자", "010-1234-1234");
        SkinCaptureFile file = jpegFile();
        when(userRepository.findByUserCode(USER_CODE)).thenReturn(Optional.of(user));
        when(skinCaptureRepository.existsByUser_UserCodeAndCapturedDateAndQualityStatus(
                USER_CODE, LocalDate.of(2026, 8, 17), SkinCaptureQualityStatus.VALID
        )).thenReturn(false);
        when(storage.store(file)).thenReturn("/images/capture.jpg");
        when(skinCaptureRepository.save(any(SkinCapture.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        SkinCapture result = service.create(USER_CODE, file);

        assertThat(result.getCapturedDate()).isEqualTo(LocalDate.of(2026, 8, 17));
        assertThat(result.getCapturedAt()).hasToString("2026-08-17T12:30");
        assertThat(result.getQualityStatus()).isEqualTo(SkinCaptureQualityStatus.VALID);
        assertThat(result.getOriginalFilename()).isEqualTo("face.jpg");
        verify(storage).store(file);
    }

    @Test
    void rejectsSecondValidCaptureOnSameKoreanDate() {
        when(userRepository.findByUserCode(USER_CODE))
                .thenReturn(Optional.of(new User(USER_CODE, "테스트 사용자", "010-1234-1234")));
        when(skinCaptureRepository.existsByUser_UserCodeAndCapturedDateAndQualityStatus(
                USER_CODE, LocalDate.of(2026, 8, 17), SkinCaptureQualityStatus.VALID
        )).thenReturn(true);

        assertThatThrownBy(() -> service.create(USER_CODE, jpegFile()))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.errorCode()).isEqualTo(ErrorCode.SKIN_CAPTURE_ALREADY_EXISTS)
                );
        verify(storage, never()).store(any());
    }

    @Test
    void rejectsUnsupportedImageTypeBeforeStorage() {
        SkinCaptureFile file = new SkinCaptureFile("face.gif", "image/gif", 3, new byte[]{1, 2, 3});

        assertThatThrownBy(() -> service.create(USER_CODE, file))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.errorCode()).isEqualTo(ErrorCode.INVALID_SKIN_CAPTURE_IMAGE)
                );
        verify(userRepository, never()).findByUserCode(any());
        verify(storage, never()).store(any());
    }

    private SkinCaptureFile jpegFile() {
        return new SkinCaptureFile(
                "folder/face.jpg",
                "image/jpeg",
                4,
                new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0}
        );
    }
}
