package com.innerderma.skinanalysis.infrastructure;

import com.innerderma.common.error.BusinessException;
import com.innerderma.common.error.ErrorCode;
import com.innerderma.skinanalysis.application.SkinAgeAnalysisResult;
import com.innerderma.skinanalysis.application.SkinAgeClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class HttpSkinAgeClient implements SkinAgeClient {

    private final RestClient restClient;

    @Autowired
    public HttpSkinAgeClient(@Value("${skinage.base-url:http://localhost:8000}") String baseUrl) {
        this(RestClient.builder(), baseUrl);
    }

    HttpSkinAgeClient(RestClient.Builder builder, String baseUrl) {
        this.restClient = builder.baseUrl(baseUrl).build();
    }

    @Override
    public SkinAgeAnalysisResult analyze(
            byte[] imageBytes,
            String filename,
            String contentType,
            Integer actualAge
    ) {
        ByteArrayResource image = new ByteArrayResource(imageBytes) {
            @Override
            public String getFilename() {
                return filename;
            }
        };

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        HttpHeaders imageHeaders = new HttpHeaders();
        imageHeaders.setContentType(MediaType.parseMediaType(contentType));
        body.add("file", new HttpEntity<>(image, imageHeaders));
        if (actualAge != null) {
            body.add("age", actualAge);
        }
        body.add("include_heatmaps", false);

        try {
            SkinAgeAnalysisResult result = restClient.post()
                    .uri("/api/v1/analyze")
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(body)
                    .retrieve()
                    .body(SkinAgeAnalysisResult.class);
            if (result == null) {
                throw new BusinessException(ErrorCode.SKINAGE_INVALID_RESPONSE);
            }
            return result;
        } catch (RestClientException exception) {
            throw new BusinessException(ErrorCode.SKINAGE_API_UNAVAILABLE);
        }
    }
}
