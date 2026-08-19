package com.innerderma.skincapture.api;

import com.innerderma.common.error.BusinessException;
import com.innerderma.common.error.ErrorCode;
import com.innerderma.common.response.ApiResponse;
import com.innerderma.skinanalysis.application.SkinAnalysisResult;
import com.innerderma.skinanalysis.application.SkinAnalysisService;
import com.innerderma.skincapture.application.SkinCaptureFile;
import com.innerderma.skincapture.application.SkinCaptureService;
import com.innerderma.skincapture.domain.SkinCapture;
import com.innerderma.skincapture.domain.SkinCaptureQualityStatus;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;

@RestController
@RequestMapping("/api/users/{userCode}/skin-captures")
public class SkinCaptureController {

    private final SkinCaptureService skinCaptureService;
    private final SkinAnalysisService skinAnalysisService;

    public SkinCaptureController(SkinCaptureService skinCaptureService, SkinAnalysisService skinAnalysisService) {
        this.skinCaptureService = skinCaptureService;
        this.skinAnalysisService = skinAnalysisService;
    }

    @PostMapping(consumes = "multipart/form-data")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<SkinCaptureResponse> create(
            @PathVariable String userCode,
            @RequestPart("file") MultipartFile file
    ) {
        try {
            SkinCaptureFile captureFile = new SkinCaptureFile(
                    file.getOriginalFilename(),
                    file.getContentType(),
                    file.getSize(),
                    file.getBytes()
            );
            return ApiResponse.success(SkinCaptureResponse.from(skinCaptureService.create(userCode, captureFile)));
        } catch (IOException exception) {
            throw new BusinessException(ErrorCode.INVALID_SKIN_CAPTURE_IMAGE);
        }
    }

    /**
     * 사진 업로드 + SkinAge 분석을 한 번에 수행하는 원스텝 API.
     * 업로드 → 품질 게이트 → VALID이면 자동으로 SkinAge 분석 호출 → 결과 반환.
     */
    @PostMapping(value = "/analyze", consumes = "multipart/form-data")
    public ApiResponse<CaptureAndAnalyzeResponse> captureAndAnalyze(
            @PathVariable String userCode,
            @RequestPart("file") MultipartFile file,
            @RequestParam(required = false) Integer actualAge
    ) {
        try {
            SkinCaptureFile captureFile = new SkinCaptureFile(
                    file.getOriginalFilename(),
                    file.getContentType(),
                    file.getSize(),
                    file.getBytes()
            );
            SkinCapture capture = skinCaptureService.create(userCode, captureFile);

            if (capture.getQualityStatus() != SkinCaptureQualityStatus.VALID) {
                return ApiResponse.success(CaptureAndAnalyzeResponse.qualityFailed(capture));
            }

            SkinAnalysisResult analysisResult = skinAnalysisService.analyze(userCode, capture.getId(), actualAge);
            return ApiResponse.success(CaptureAndAnalyzeResponse.success(capture, analysisResult));
        } catch (IOException exception) {
            throw new BusinessException(ErrorCode.INVALID_SKIN_CAPTURE_IMAGE);
        }
    }

    @GetMapping("/latest")
    public ApiResponse<SkinCaptureResponse> getLatest(@PathVariable String userCode) {
        return ApiResponse.success(SkinCaptureResponse.from(skinCaptureService.getLatest(userCode)));
    }

    @GetMapping("/today")
    public ApiResponse<DailyCaptureStatusResponse> getToday(@PathVariable String userCode) {
        return ApiResponse.success(DailyCaptureStatusResponse.from(skinCaptureService.getTodayStatus(userCode)));
    }

    @GetMapping("/history")
    public ApiResponse<SkinCaptureHistoryResponse> getHistory(
            @PathVariable String userCode,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ApiResponse.success(SkinCaptureHistoryResponse.from(
                skinCaptureService.getHistory(userCode, from, to)));
    }
}
