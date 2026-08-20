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
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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

@Tag(name = "Skin Capture", description = "피부 사진 촬영 — 업로드, 이력, 분석 원스텝")
@RestController
@RequestMapping("/api/users/{userCode}/skin-captures")
public class SkinCaptureController {

    private final SkinCaptureService skinCaptureService;
    private final SkinAnalysisService skinAnalysisService;

    public SkinCaptureController(SkinCaptureService skinCaptureService, SkinAnalysisService skinAnalysisService) {
        this.skinCaptureService = skinCaptureService;
        this.skinAnalysisService = skinAnalysisService;
    }

    @Operation(summary = "피부 사진 업로드", description = "피부 사진을 업로드합니다. 품질 게이트를 통과해야 VALID 상태가 됩니다.")
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

    @Operation(summary = "사진 업로드 + 분석 원스텝", description = "사진 업로드 후 품질 통과 시 자동으로 SkinAge 분석까지 수행합니다.")
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

            try {
                SkinAnalysisResult analysisResult = skinAnalysisService.analyze(userCode, capture.getId(), actualAge);
                return ApiResponse.success(CaptureAndAnalyzeResponse.success(capture, analysisResult));
            } catch (BusinessException | com.innerderma.skinanalysis.application.SkinAgeQualityCheckFailedException analysisEx) {
                // SkinAge 분석 실패(얼굴 인식 실패 등): capture를 QUALITY_CHECK_FAILED로 변경해
                // daily limit에서 제외하고 재촬영 가능하게 함
                skinCaptureService.markAnalysisFailed(capture.getId());
                throw analysisEx;  // GlobalExceptionHandler에서 적절한 에러 코드로 변환
            }
        } catch (IOException exception) {
            throw new BusinessException(ErrorCode.INVALID_SKIN_CAPTURE_IMAGE);
        }
    }

    @Operation(summary = "최신 촬영 조회")
    @GetMapping("/latest")
    public ApiResponse<SkinCaptureResponse> getLatest(@PathVariable String userCode) {
        return ApiResponse.success(SkinCaptureResponse.from(skinCaptureService.getLatest(userCode)));
    }

    @Operation(summary = "오늘 촬영 상태")
    @GetMapping("/today")
    public ApiResponse<DailyCaptureStatusResponse> getToday(@PathVariable String userCode) {
        return ApiResponse.success(DailyCaptureStatusResponse.from(skinCaptureService.getTodayStatus(userCode)));
    }

    @Operation(summary = "촬영 이력 조회", description = "기간별 유효 촬영 기록을 조회합니다 (최대 31일).")
    @GetMapping("/history")
    public ApiResponse<SkinCaptureHistoryResponse> getHistory(
            @PathVariable String userCode,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ApiResponse.success(SkinCaptureHistoryResponse.from(
                skinCaptureService.getHistory(userCode, from, to)));
    }
}
