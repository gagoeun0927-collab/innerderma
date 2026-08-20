package com.innerderma.procedure.api;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;

/**
 * 시술 여부 등록 요청.
 *
 * <p>프론트는 "시술 받으셨나요?" 응답과 시술 종류·날짜만 보내면 된다.
 * 회복 기간·정상/경고 증상·주의사항·허용/제한 제품 태그는 백엔드가
 * Treatment Knowledge Base에서 조회해 채운다. 임상 값을 클라이언트가 만들지 않는다.
 *
 * @param hadProcedure   시술 받았는지 여부. false면 기록을 만들지 않는다.
 * @param treatmentCode  시술 코드 (Treatment KB 기준, 예: laser_toning). hadProcedure=true면 필수
 * @param procedureDate  시술 날짜. 생략 시 오늘로 처리
 * @param facilityCode   시술 기관 코드. 생략 시 기본 기관(WHS) 사용
 */
@Schema(description = "시술 여부 등록 요청")
public record ProcedureRegisterRequest(
        @Schema(description = "시술 받았는지 여부. false면 기록을 생성하지 않습니다.", example = "true")
        Boolean hadProcedure,

        @Schema(description = "시술 코드 (Treatment KB 기준). hadProcedure=true일 때 필수",
                example = "laser_toning")
        String treatmentCode,

        @Schema(description = "시술 날짜 (yyyy-MM-dd). 생략 시 오늘", example = "2026-08-20")
        LocalDate procedureDate,

        @Schema(description = "시술 기관 코드. 생략 시 WHS", example = "WHS")
        String facilityCode
) {
    public boolean hadProcedureOrDefault() {
        return hadProcedure != null && hadProcedure;
    }
}
