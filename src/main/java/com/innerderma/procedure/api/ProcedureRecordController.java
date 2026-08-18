package com.innerderma.procedure.api;

import com.innerderma.common.response.ApiResponse;
import com.innerderma.procedure.application.ProcedureRecordService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/users/{userCode}/procedures")
public class ProcedureRecordController {

    private final ProcedureRecordService procedureRecordService;

    public ProcedureRecordController(ProcedureRecordService procedureRecordService) {
        this.procedureRecordService = procedureRecordService;
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
