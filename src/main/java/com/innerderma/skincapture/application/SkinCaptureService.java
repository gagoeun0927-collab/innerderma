package com.innerderma.skincapture.application;

import com.innerderma.common.error.BusinessException;
import com.innerderma.common.error.ErrorCode;
import com.innerderma.skincapture.domain.SkinCapture;
import com.innerderma.skincapture.domain.SkinCaptureQualityStatus;
import com.innerderma.skincapture.domain.SkinCaptureRepository;
import com.innerderma.user.domain.User;
import com.innerderma.user.domain.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Iterator;
import java.util.Set;

@Service
@Transactional(readOnly = true)
public class SkinCaptureService {

    static final long MAX_IMAGE_SIZE = 10L * 1024 * 1024;
    static final int MIN_RESOLUTION = 512;
    private static final long MAX_RANGE_DAYS = 31;
    private static final ZoneId MVP_ZONE = ZoneId.of("Asia/Seoul");
    private static final Set<String> SUPPORTED_CONTENT_TYPES = Set.of("image/jpeg", "image/png", "image/webp");

    private final SkinCaptureRepository skinCaptureRepository;
    private final UserRepository userRepository;
    private final SkinCaptureStorage storage;
    private final Clock clock;

    /**
     * 촬영 1일 1회 제한 사용 여부 (기획서 7.1 R002).
     * 기본값 false — 같은 날 재촬영을 허용하고, 최신 촬영·분석 결과로 갱신한다.
     * true로 두면 그날 VALID 촬영이 있으면 CAPTURE_002로 막는다.
     */
    private final boolean dailyLimitEnabled;

    @Autowired
    public SkinCaptureService(
            SkinCaptureRepository skinCaptureRepository,
            UserRepository userRepository,
            SkinCaptureStorage storage,
            @org.springframework.beans.factory.annotation.Value(
                    "${innerderma.skin-capture.daily-limit-enabled:false}") boolean dailyLimitEnabled
    ) {
        this(skinCaptureRepository, userRepository, storage, Clock.system(MVP_ZONE), dailyLimitEnabled);
    }

    SkinCaptureService(
            SkinCaptureRepository skinCaptureRepository,
            UserRepository userRepository,
            SkinCaptureStorage storage,
            Clock clock
    ) {
        this(skinCaptureRepository, userRepository, storage, clock, false);
    }

    SkinCaptureService(
            SkinCaptureRepository skinCaptureRepository,
            UserRepository userRepository,
            SkinCaptureStorage storage,
            Clock clock,
            boolean dailyLimitEnabled
    ) {
        this.skinCaptureRepository = skinCaptureRepository;
        this.userRepository = userRepository;
        this.storage = storage;
        this.clock = clock;
        this.dailyLimitEnabled = dailyLimitEnabled;
    }

    @Transactional
    public SkinCapture create(String userCode, SkinCaptureFile file) {
        validate(file);
        User user = userRepository.findByUserCode(userCode)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        LocalDate capturedDate = LocalDate.now(clock);

        // 1일 1회 제한(기획서 7.1 R002). 기본은 해제 — 같은 날 재촬영 시 최신 결과로 갱신한다.
        // 하위 흐름(분석 → care-cycle → care-solution)은 각각 분석 ID·사이클 ID 기준으로
        // 중복을 판단하므로, 새 촬영이 생기면 최신 분석 기준으로 자연히 다시 생성된다.
        if (dailyLimitEnabled) {
            boolean alreadyCaptured = skinCaptureRepository.existsByUser_UserCodeAndCapturedDateAndQualityStatus(
                    userCode,
                    capturedDate,
                    SkinCaptureQualityStatus.VALID
            );
            if (alreadyCaptured) {
                throw new BusinessException(ErrorCode.SKIN_CAPTURE_ALREADY_EXISTS);
            }
        }

        SkinCaptureQualityStatus qualityStatus = assessQuality(file);
        String imagePath = storage.store(file);
        SkinCapture capture = new SkinCapture(
                user,
                capturedDate,
                LocalDateTime.now(clock),
                imagePath,
                safeOriginalFilename(file.originalFilename()),
                file.contentType(),
                file.size(),
                qualityStatus
        );
        return skinCaptureRepository.save(capture);
    }

    public SkinCapture getLatest(String userCode) {
        if (!userRepository.existsByUserCode(userCode)) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        return skinCaptureRepository.findFirstByUser_UserCodeOrderByCapturedAtDesc(userCode)
                .orElseThrow(() -> new BusinessException(ErrorCode.SKIN_CAPTURE_NOT_FOUND));
    }

    /**
     * SkinAge 분석 실패 시 capture를 QUALITY_CHECK_FAILED로 변경해
     * daily limit에서 제외한다 (같은 날 재촬영 가능).
     */
    @Transactional
    public void markAnalysisFailed(Long captureId) {
        skinCaptureRepository.findById(captureId).ifPresent(capture -> {
            capture.markQualityFailed();
            skinCaptureRepository.save(capture);
        });
    }

    public DailyCaptureStatus getTodayStatus(String userCode) {
        if (!userRepository.existsByUserCode(userCode)) throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        LocalDate today = LocalDate.now(clock);
        var capture = skinCaptureRepository
                .findFirstByUser_UserCodeAndCapturedDateAndQualityStatusOrderByCapturedAtDesc(
                        userCode, today, SkinCaptureQualityStatus.VALID).orElse(null);
        return new DailyCaptureStatus(today, capture == null, capture);
    }

    /**
     * R002 품질 게이트 (기획서 7.1): 최소 해상도 미만이거나 디코딩에 실패한 사진은 예외가 아니라
     * QUALITY_CHECK_FAILED 상태로 저장해 재촬영을 유도한다. VALID 촬영만 1일 1회 제한에 반영되므로
     * 품질 미달 사진은 같은 날 재촬영을 막지 않는다. 표준 JDK가 디코더를 제공하지 않는 형식(webp 등)은
     * 해상도를 측정할 수 없으므로 오탐 방지를 위해 VALID로 통과시킨다.
     */
    private SkinCaptureQualityStatus assessQuality(SkinCaptureFile file) {
        try (ImageInputStream stream = ImageIO.createImageInputStream(new ByteArrayInputStream(file.bytes()))) {
            if (stream == null) {
                return SkinCaptureQualityStatus.QUALITY_CHECK_FAILED;
            }
            Iterator<ImageReader> readers = ImageIO.getImageReaders(stream);
            if (!readers.hasNext()) {
                return SkinCaptureQualityStatus.VALID;
            }
            ImageReader reader = readers.next();
            try {
                reader.setInput(stream, true, true);
                int minEdge = Math.min(reader.getWidth(0), reader.getHeight(0));
                return minEdge >= MIN_RESOLUTION
                        ? SkinCaptureQualityStatus.VALID
                        : SkinCaptureQualityStatus.QUALITY_CHECK_FAILED;
            } finally {
                reader.dispose();
            }
        } catch (IOException | RuntimeException exception) {
            return SkinCaptureQualityStatus.QUALITY_CHECK_FAILED;
        }
    }

    public SkinCaptureHistoryResult getHistory(String userCode, LocalDate from, LocalDate to) {
        if (!userRepository.existsByUserCode(userCode)) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        LocalDate resolvedTo = to == null ? LocalDate.now(clock) : to;
        LocalDate resolvedFrom = from == null ? resolvedTo.minusDays(29) : from;
        if (resolvedFrom.isAfter(resolvedTo)
                || ChronoUnit.DAYS.between(resolvedFrom, resolvedTo) + 1 > MAX_RANGE_DAYS) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }
        var items = skinCaptureRepository
                .findByUser_UserCodeAndCapturedDateBetweenAndQualityStatusOrderByCapturedDateDescCapturedAtDesc(
                        userCode, resolvedFrom, resolvedTo, SkinCaptureQualityStatus.VALID);
        return new SkinCaptureHistoryResult(resolvedFrom, resolvedTo, items);
    }

    private void validate(SkinCaptureFile file) {
        if (file == null || file.bytes() == null || file.size() <= 0 || file.size() != file.bytes().length) {
            throw new BusinessException(ErrorCode.INVALID_SKIN_CAPTURE_IMAGE);
        }
        if (file.size() > MAX_IMAGE_SIZE || !SUPPORTED_CONTENT_TYPES.contains(file.contentType())) {
            throw new BusinessException(ErrorCode.INVALID_SKIN_CAPTURE_IMAGE);
        }
        if (!hasExpectedSignature(file.contentType(), file.bytes())) {
            throw new BusinessException(ErrorCode.INVALID_SKIN_CAPTURE_IMAGE);
        }
    }

    private boolean hasExpectedSignature(String contentType, byte[] bytes) {
        return switch (contentType) {
            case "image/jpeg" -> bytes.length >= 3
                    && (bytes[0] & 0xFF) == 0xFF
                    && (bytes[1] & 0xFF) == 0xD8
                    && (bytes[2] & 0xFF) == 0xFF;
            case "image/png" -> bytes.length >= 8
                    && (bytes[0] & 0xFF) == 0x89
                    && bytes[1] == 0x50 && bytes[2] == 0x4E && bytes[3] == 0x47
                    && bytes[4] == 0x0D && bytes[5] == 0x0A && bytes[6] == 0x1A && bytes[7] == 0x0A;
            case "image/webp" -> bytes.length >= 12
                    && bytes[0] == 'R' && bytes[1] == 'I' && bytes[2] == 'F' && bytes[3] == 'F'
                    && bytes[8] == 'W' && bytes[9] == 'E' && bytes[10] == 'B' && bytes[11] == 'P';
            default -> false;
        };
    }

    private String safeOriginalFilename(String filename) {
        if (filename == null || filename.isBlank()) {
            return "capture";
        }
        String normalized = filename.replace('\\', '/');
        String basename = normalized.substring(normalized.lastIndexOf('/') + 1);
        return basename.length() <= 255 ? basename : basename.substring(basename.length() - 255);
    }
}
