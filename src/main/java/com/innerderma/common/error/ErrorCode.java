package com.innerderma.common.error;

import org.springframework.http.HttpStatus;

public enum ErrorCode {
    INVALID_REQUEST(HttpStatus.BAD_REQUEST, "COMMON_001", "요청값이 올바르지 않습니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "COMMON_002", "서버 내부 오류가 발생했습니다."),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "USER_001", "사용자를 찾을 수 없습니다."),
    SKIN_DIAGNOSIS_NOT_FOUND(HttpStatus.NOT_FOUND, "SKIN_001", "피부 진단 결과를 찾을 수 없습니다."),
    PROCEDURE_NOT_FOUND(HttpStatus.NOT_FOUND, "PROCEDURE_001", "시술 기록을 찾을 수 없습니다."),
    INVALID_SKIN_CAPTURE_IMAGE(HttpStatus.BAD_REQUEST, "CAPTURE_001", "지원하지 않는 피부 사진입니다."),
    SKIN_CAPTURE_ALREADY_EXISTS(HttpStatus.CONFLICT, "CAPTURE_002", "오늘의 유효한 피부 촬영 기록이 이미 있습니다."),
    SKIN_CAPTURE_NOT_FOUND(HttpStatus.NOT_FOUND, "CAPTURE_003", "피부 촬영 기록을 찾을 수 없습니다."),
    SKIN_CAPTURE_STORAGE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "CAPTURE_004", "피부 사진을 저장하지 못했습니다."),
    SELF_CHECK_NOT_FOUND(HttpStatus.NOT_FOUND, "SELF_CHECK_001", "자가 피부 상태 기록을 찾을 수 없습니다."),
    SKIN_ANALYSIS_NOT_FOUND(HttpStatus.NOT_FOUND, "ANALYSIS_001", "피부 사진 분석 결과를 찾을 수 없습니다."),
    SKIN_ANALYSIS_ALREADY_EXISTS(HttpStatus.CONFLICT, "ANALYSIS_002", "해당 사진의 분석 결과가 이미 있습니다."),
    SKIN_ANALYSIS_IMAGE_NOT_AVAILABLE(HttpStatus.UNPROCESSABLE_CONTENT, "ANALYSIS_003", "분석할 피부 사진을 불러올 수 없습니다."),
    SKINAGE_API_UNAVAILABLE(HttpStatus.BAD_GATEWAY, "ANALYSIS_004", "피부 분석 서비스에 연결할 수 없습니다."),
    SKINAGE_INVALID_RESPONSE(HttpStatus.BAD_GATEWAY, "ANALYSIS_005", "피부 분석 서비스의 응답이 올바르지 않습니다."),
    CARE_CYCLE_NOT_FOUND(HttpStatus.NOT_FOUND, "CARE_CYCLE_001", "사용할 수 있는 케어 사이클을 찾을 수 없습니다."),
    CARE_CYCLE_ALREADY_EXISTS(HttpStatus.CONFLICT, "CARE_CYCLE_002", "해당 피부 분석의 케어 사이클이 이미 있습니다."),
    CARE_SOLUTION_NOT_FOUND(HttpStatus.NOT_FOUND, "CARE_SOLUTION_001", "케어 솔루션을 찾을 수 없습니다."),
    CARE_SOLUTION_ALREADY_EXISTS(HttpStatus.CONFLICT, "CARE_SOLUTION_002", "해당 케어 사이클의 솔루션이 이미 있습니다."),
    CARE_HISTORY_NOT_FOUND(HttpStatus.NOT_FOUND, "CARE_HISTORY_001", "해당 날짜의 케어 기록을 찾을 수 없습니다."),
    SKIN_STATE_SNAPSHOT_NOT_FOUND(HttpStatus.NOT_FOUND, "SNAPSHOT_001", "피부 상태 스냅샷을 찾을 수 없습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;

    ErrorCode(HttpStatus status, String code, String message) {
        this.status = status;
        this.code = code;
        this.message = message;
    }

    public HttpStatus status() {
        return status;
    }

    public String code() {
        return code;
    }

    public String message() {
        return message;
    }
}
