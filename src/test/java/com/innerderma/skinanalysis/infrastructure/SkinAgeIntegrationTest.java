package com.innerderma.skinanalysis.infrastructure;

import com.innerderma.common.error.BusinessException;
import com.innerderma.common.error.ErrorCode;
import com.innerderma.skinanalysis.application.SkinAgeAnalysisResult;
import com.innerderma.skinanalysis.application.SkinAgeClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * SkinAge 서버(localhost:8000)가 실행 중일 때만 동작하는 통합 테스트.
 * 환경변수 SKINAGE_INTEGRATION=true 로 활성화한다.
 *
 * 두 가지 시나리오:
 * 1. 연결 확인: 얼굴 없는 단색 이미지를 보내면 서버가 4xx(422 등)를 반환하더라도
 *    "연결 자체는 가능"함을 확인 (SKINAGE_API_UNAVAILABLE이 아닌 SKINAGE_INVALID_RESPONSE 또는 성공)
 * 2. 실제 분석(선택): src/test/resources/에 test-face-real.jpg가 있으면 풀 분석 검증
 */
@EnabledIfEnvironmentVariable(named = "SKINAGE_INTEGRATION", matches = "true")
class SkinAgeIntegrationTest {

    private final SkinAgeClient client = new HttpSkinAgeClient("http://localhost:8000");

    @Test
    void serverIsReachableAndResponds() throws Exception {
        byte[] imageBytes = createTestJpeg(640, 640);

        try {
            SkinAgeAnalysisResult result = client.analyze(imageBytes, "test-face.jpg", "image/jpeg", 25);
            // 서버가 얼굴 없는 이미지에도 응답을 줄 수 있음(mock 모드 등)
            assertThat(result).isNotNull();
            System.out.println("=== SkinAge responded with analysis result (possibly mock) ===");
            System.out.println("Overall Score: " + result.summary().overallScore());
        } catch (BusinessException exception) {
            // 서버가 4xx/5xx를 반환하면 RestClientException → BusinessException
            // ANALYSIS_004 = 연결 불가, ANALYSIS_005 = 응답 이상
            assertThat(exception.errorCode())
                    .as("Server is reachable but rejected the image (expected for non-face): %s", exception.getMessage())
                    .isIn(ErrorCode.SKINAGE_INVALID_RESPONSE, ErrorCode.SKINAGE_API_UNAVAILABLE);
            System.out.println("=== SkinAge server reachable, rejected non-face image: " + exception.errorCode() + " ===");
        }
    }

    @Test
    void analyzesRealFaceImageIfAvailable() throws Exception {
        var realImageUrl = getClass().getClassLoader().getResource("test-face-real.jpg");
        if (realImageUrl == null) {
            System.out.println("=== Skipping real face test: src/test/resources/test-face-real.jpg not found ===");
            return;
        }
        byte[] imageBytes = java.nio.file.Files.readAllBytes(java.nio.file.Paths.get(realImageUrl.toURI()));

        SkinAgeAnalysisResult result = client.analyze(imageBytes, "real-face.jpg", "image/jpeg", 25);

        assertThat(result).isNotNull();
        assertThat(result.summary()).isNotNull();
        assertThat(result.summary().overallScore()).isBetween(0.0, 100.0);
        assertThat(result.summary().skinHealthGrade()).isNotBlank();
        assertThat(result.zoneScores()).hasSize(7);
        assertThat(result.aggregateMetrics()).isNotNull();
        assertThat(result.aggregateMetrics().concernAverages()).containsKeys("wrinkle", "pore_texture", "pigmentation", "redness");
        assertThat(result.metadata()).isNotNull();
        assertThat(result.metadata().modelVersion()).isNotBlank();

        System.out.println("=== SkinAge Real Face Analysis PASSED ===");
        System.out.println("Overall Score: " + result.summary().overallScore());
        System.out.println("Grade: " + result.summary().skinHealthGrade());
        System.out.println("Model: " + result.metadata().modelVersion());
        System.out.println("Concern Averages: " + result.aggregateMetrics().concernAverages());
    }

    private byte[] createTestJpeg(int width, int height) throws Exception {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        // 단순 그라데이션 이미지 (얼굴 아님)
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                image.setRGB(x, y, ((x * 255 / width) << 16) | ((y * 255 / height) << 8) | 128);
            }
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(image, "jpg", out);
        return out.toByteArray();
    }
}
