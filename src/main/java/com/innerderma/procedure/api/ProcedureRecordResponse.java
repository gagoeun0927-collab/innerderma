package com.innerderma.procedure.api;

import com.innerderma.procedure.domain.ProcedureRecord;

import java.time.LocalDate;

public record ProcedureRecordResponse(
        Long id,
        String facilityCode,
        String facilityName,
        LocalDate procedureDate,
        String procedureName,
        String careGuide
) {
    public static ProcedureRecordResponse from(ProcedureRecord record) {
        return new ProcedureRecordResponse(
                record.getId(),
                record.getFacility().getFacilityCode(),
                record.getFacility().getName(),
                record.getProcedureDate(),
                record.getProcedureName(),
                record.getCareGuide()
        );
    }
}
