package com.innerderma.procedure.api;

import com.innerderma.common.response.ApiResponse;
import com.innerderma.procedure.application.ProcedureRecordService;
import com.innerderma.procedure.domain.ProcedureRecord;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@Tag(name = "Procedure", description = "시술 기록 — 시술 여부 등록, 조회, 시술 컨텍스트")
@RestController
@RequestMapping("/api/users/{userCode}/procedures")
public class ProcedureRecordController {

    private final ProcedureRecordService procedureRecordService;

    public ProcedureRecordController(ProcedureRecordService procedureRecordService) {
        this.procedureRecordService = procedureRecordService;
    }

    @Operation(
            summary = "시술 여부 등록",
            description = "사용자의 시술 여부를 등록합니다. hadProcedure=true면 시술 기록을 생성하고, "
                    + "false면 기록을 만들지 않습니다(시술 기록이 없는 상태가 미시술 상태입니다).\n\n"
                    + "회복 기간·정상/경고 증상·주의사항·허용/제한 제품 태그는 서버가 Treatment Knowledge Base에서 "
                    + "조회해 채우므로 클라이언트는 시술 코드와 날짜만 보내면 됩니다.\n\n"
                    + "등록 시 해당 사용자의 AI Care 캐시가 무효화되어, 다음 /ai-care 호출에서 시술 정보가 반영됩니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "등록 성공 (hadProcedure=false면 data=null)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "지원하지 않는 시술 코드(PROCEDURE_003) 또는 treatmentCode 누락"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "사용자 또는 시술 기관을 찾을 수 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "같은 기관·날짜의 시술 기록이 이미 있음(PROCEDURE_002)")
    })
    @PostMapping
    public ApiResponse<ProcedureRecordResponse> registerProcedure(
            @PathVariable String userCode,
            @Valid @RequestBody ProcedureRegisterRequest request
    ) {
        ProcedureRecord saved = procedureRecordService.register(
                userCode,
                request.hadProcedureOrDefault(),
                request.treatmentCode(),
                request.procedureDate(),
                request.facilityCode()
        );
        return ApiResponse.success(saved == null ? null : ProcedureRecordResponse.from(saved));
    }

    @Operation(
            summary = "등록 가능한 시술 목록",
            description = "시술 선택 UI에 사용할 시술 목록을 반환합니다. Treatment Knowledge Base 기준입니다."
    )
    @GetMapping("/available-treatments")
    public ApiResponse<List<AvailableTreatmentResponse>> getAvailableTreatments(
            @PathVariable String userCode
    ) {
        return ApiResponse.success(procedureRecordService.getAvailableTreatments().stream()
                .map(AvailableTreatmentResponse::from)
                .toList());
    }

    @GetMapping
    public ApiResponse<List<ProcedureRecordResponse>> getProcedureRecords(
            @PathVariable String userCode,
            @RequestParam("facilityCode") String facilityCode,
            @RequestParam("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        List<ProcedureRecordResponse> records = procedureRecordService
                .getProcedureRecords(userCode, facilityCode, date)
                .stream()
                .map(ProcedureRecordResponse::from)
                .toList();
        return ApiResponse.success(records);
    }

    @GetMapping("/treatment-context")
    public ApiResponse<TreatmentContextResponse> getTreatmentContext(
            @PathVariable String userCode,
            @RequestParam("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        return ApiResponse.success(TreatmentContextResponse.from(
                procedureRecordService.getTreatmentContext(userCode, date)));
    }

    @GetMapping("/{id}")
    public ApiResponse<ProcedureRecordResponse> getProcedureRecord(
            @PathVariable String userCode,
            @PathVariable Long id
    ) {
        return ApiResponse.success(ProcedureRecordResponse.from(
                procedureRecordService.getProcedureRecord(userCode, id)));
    }
}
