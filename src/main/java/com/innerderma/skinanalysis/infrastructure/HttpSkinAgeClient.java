package com.innerderma.skinanalysis.infrastructure;

import com.innerderma.common.error.BusinessException;
import com.innerderma.common.error.ErrorCode;
import com.innerderma.skinanalysis.application.SkinAgeAnalysisResult;
import com.innerderma.skinanalysis.application.SkinAgeClient;
import com.innerderma.skinanalysis.application.SkinAgeQualityCheckFailedException;
import com.innerderma.skinanalysis.application.SkinAgeQualityCheckResult;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * Java 11+ HttpClient 기반 SkinAge API 클라이언트.
 * Spring RestClient의 multipart 인코딩이 FastAPI와 호환되지 않는 문제를 해결하기 위해
 * 표준 java.net.http.HttpClient로 multipart/form-data를 직접 구성한다.
 */
@Component
public class HttpSkinAgeClient implements SkinAgeClient {

    private final String baseUrl;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    @Autowired
    public HttpSkinAgeClient(@Value("${skinage.base-url:http://localhost:8000}") String baseUrl,
                             ObjectMapper objectMapper) {
        this(baseUrl, objectMapper, HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).build());
    }

    HttpSkinAgeClient(String baseUrl, ObjectMapper objectMapper, HttpClient httpClient) {
        this.baseUrl = baseUrl;
        this.objectMapper = objectMapper;
        this.httpClient = httpClient;
    }

    @Override
    public SkinAgeAnalysisResult analyze(
            byte[] imageBytes,
            String filename,
            String contentType,
            Integer actualAge
    ) {
        String boundary = UUID.randomUUID().toString();
        byte[] body = buildMultipartBody(boundary, imageBytes, filename, contentType, actualAge);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/api/v1/analyze"))
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .POST(HttpRequest.BodyPublishers.ofByteArray(body))
                .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 422) {
                SkinAgeQualityCheckResult qualityResult = parseQualityFailure(response.body());
                if (qualityResult != null) {
                    throw new SkinAgeQualityCheckFailedException(qualityResult);
                }
                // face detection 실패 등 detail이 단순 문자열인 경우
                String faceMessage = parseFaceDetectionFailure(response.body());
                if (faceMessage != null) {
                    throw new BusinessException(ErrorCode.SKIN_CAPTURE_FACE_NOT_DETECTED);
                }
                throw new BusinessException(ErrorCode.SKINAGE_INVALID_RESPONSE);
            }
            if (response.statusCode() != 200) {
                throw new BusinessException(ErrorCode.SKINAGE_API_UNAVAILABLE);
            }
            SkinAgeAnalysisResult result = objectMapper.readValue(response.body(), SkinAgeAnalysisResult.class);
            if (result == null) {
                throw new BusinessException(ErrorCode.SKINAGE_INVALID_RESPONSE);
            }
            return result;
        } catch (BusinessException | SkinAgeQualityCheckFailedException passthrough) {
            throw passthrough;
        } catch (IOException | InterruptedException exception) {
            if (exception instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            throw new BusinessException(ErrorCode.SKINAGE_API_UNAVAILABLE);
        }
    }

    private SkinAgeQualityCheckResult parseQualityFailure(String body) {
        try {
            var tree = objectMapper.readTree(body);
            var detail = tree.get("detail");
            if (detail == null) return null;
            // FastAPI validation error format: detail is array
            if (detail.isArray()) return null;
            // SkinAge face detection failure: detail is string
            if (detail.isTextual()) return null;
            // SkinAge quality check format: detail is object with error/failed_checks
            var error = detail.get("error");
            if (error == null || !"quality_check_failed".equals(error.asText())) return null;
            var failedChecks = detail.get("failed_checks");
            var messages = detail.get("messages");
            java.util.List<String> checks = new java.util.ArrayList<>();
            java.util.List<String> msgs = new java.util.ArrayList<>();
            if (failedChecks != null && failedChecks.isArray()) {
                for (var node : failedChecks) checks.add(node.asText());
            }
            if (messages != null && messages.isArray()) {
                for (var node : messages) msgs.add(node.asText());
            }
            return new SkinAgeQualityCheckResult(checks, msgs);
        } catch (Exception exception) {
            return null;
        }
    }

    /**
     * SkinAge가 얼굴을 인식하지 못했을 때의 응답 파싱.
     * 형식: {"detail": "Could not detect face landmarks..."} (detail이 단순 문자열)
     */
    private String parseFaceDetectionFailure(String body) {
        try {
            var tree = objectMapper.readTree(body);
            var detail = tree.get("detail");
            if (detail != null && detail.isTextual()) {
                String message = detail.asText();
                if (message.toLowerCase().contains("face")
                        || message.toLowerCase().contains("landmark")) {
                    return message;
                }
            }
            return null;
        } catch (Exception exception) {
            return null;
        }
    }

    private byte[] buildMultipartBody(String boundary, byte[] imageBytes, String filename,
                                      String contentType, Integer actualAge) {
        StringBuilder sb = new StringBuilder();

        // file part
        sb.append("--").append(boundary).append("\r\n");
        sb.append("Content-Disposition: form-data; name=\"file\"; filename=\"").append(filename).append("\"\r\n");
        sb.append("Content-Type: ").append(contentType).append("\r\n");
        sb.append("\r\n");

        byte[] prefix = sb.toString().getBytes(StandardCharsets.UTF_8);

        StringBuilder suffix = new StringBuilder();
        suffix.append("\r\n");

        // include_heatmaps part
        suffix.append("--").append(boundary).append("\r\n");
        suffix.append("Content-Disposition: form-data; name=\"include_heatmaps\"\r\n");
        suffix.append("\r\n");
        suffix.append("false\r\n");

        // age part (optional)
        if (actualAge != null) {
            suffix.append("--").append(boundary).append("\r\n");
            suffix.append("Content-Disposition: form-data; name=\"age\"\r\n");
            suffix.append("\r\n");
            suffix.append(actualAge).append("\r\n");
        }

        suffix.append("--").append(boundary).append("--\r\n");
        byte[] suffixBytes = suffix.toString().getBytes(StandardCharsets.UTF_8);

        byte[] result = new byte[prefix.length + imageBytes.length + suffixBytes.length];
        System.arraycopy(prefix, 0, result, 0, prefix.length);
        System.arraycopy(imageBytes, 0, result, prefix.length, imageBytes.length);
        System.arraycopy(suffixBytes, 0, result, prefix.length + imageBytes.length, suffixBytes.length);
        return result;
    }
}
