package com.innerderma.procedure.application;

import com.innerderma.common.error.BusinessException;
import com.innerderma.common.error.ErrorCode;
import com.innerderma.procedure.domain.ProcedureRecord;
import com.innerderma.procedure.domain.ProcedureRecordRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class ProcedureRecordService {

    private final ProcedureRecordRepository procedureRecordRepository;

    public ProcedureRecordService(ProcedureRecordRepository procedureRecordRepository) {
        this.procedureRecordRepository = procedureRecordRepository;
    }

    public List<ProcedureRecord> getProcedureRecords(
            String userCode,
            String facilityCode,
            LocalDate procedureDate
    ) {
        List<ProcedureRecord> records = procedureRecordRepository
                .findAllByUser_UserCodeAndFacility_FacilityCodeAndProcedureDate(
                        userCode,
                        facilityCode,
                        procedureDate
                );

        if (records.isEmpty()) {
            throw new BusinessException(ErrorCode.PROCEDURE_NOT_FOUND);
        }
        return records;
    }

    public TreatmentContext getTreatmentContext(String userCode, LocalDate referenceDate) {
        ProcedureRecord record = procedureRecordRepository
                .findFirstByUser_UserCodeAndProcedureDateLessThanEqualOrderByProcedureDateDescIdDesc(
                        userCode, referenceDate)
                .orElseThrow(() -> new BusinessException(ErrorCode.PROCEDURE_NOT_FOUND));
        return TreatmentContext.from(record, referenceDate);
    }
}
