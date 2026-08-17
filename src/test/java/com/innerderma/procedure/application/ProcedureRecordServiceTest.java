package com.innerderma.procedure.application;

import com.innerderma.common.error.BusinessException;
import com.innerderma.facility.domain.Facility;
import com.innerderma.procedure.domain.ProcedureRecord;
import com.innerderma.procedure.domain.ProcedureRecordRepository;
import com.innerderma.user.domain.User;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class ProcedureRecordServiceTest {
    private static final String USER_CODE = "WHS-DEMO-001";

    @Test
    void returnsLatestProcedureOnOrBeforeReferenceDateAndComputesElapsedDays() {
        ProcedureRecordRepository repository = mock(ProcedureRecordRepository.class);
        ProcedureRecordService service = new ProcedureRecordService(repository);
        LocalDate referenceDate = LocalDate.of(2026, 8, 18);
        ProcedureRecord record = structuredRecord(LocalDate.of(2026, 8, 15));
        when(repository.findFirstByUser_UserCodeAndProcedureDateLessThanEqualOrderByProcedureDateDescIdDesc(
                USER_CODE, referenceDate)).thenReturn(Optional.of(record));

        TreatmentContext context = service.getTreatmentContext(USER_CODE, referenceDate);

        assertThat(context.daysSinceTreatment()).isEqualTo(3);
        assertThat(context.treatmentCode()).isEqualTo("VERIFIED-CODE");
        assertThat(context.expectedRecoveryDaysMin()).isEqualTo(2);
        assertThat(context.expectedRecoveryDaysMax()).isEqualTo(5);
        assertThat(context.normalSymptoms()).containsExactly("검증된 정상 증상");
        verify(repository).findFirstByUser_UserCodeAndProcedureDateLessThanEqualOrderByProcedureDateDescIdDesc(
                USER_CODE, referenceDate);
    }

    @Test
    void doesNotSelectFutureProcedureBecauseRepositoryIsBoundedByReferenceDate() {
        ProcedureRecordRepository repository = mock(ProcedureRecordRepository.class);
        ProcedureRecordService service = new ProcedureRecordService(repository);
        LocalDate referenceDate = LocalDate.of(2026, 8, 18);
        when(repository.findFirstByUser_UserCodeAndProcedureDateLessThanEqualOrderByProcedureDateDescIdDesc(
                USER_CODE, referenceDate)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getTreatmentContext(USER_CODE, referenceDate))
                .isInstanceOf(BusinessException.class);
        verify(repository).findFirstByUser_UserCodeAndProcedureDateLessThanEqualOrderByProcedureDateDescIdDesc(
                USER_CODE, referenceDate);
    }

    private ProcedureRecord structuredRecord(LocalDate date) {
        User user = new User(USER_CODE, "테스트 사용자", "010-1234-1234");
        Facility facility = new Facility("WHS", "웰니스 하우스 서울");
        return new ProcedureRecord(user, facility, date, "기존 시술명", "기존 관리 가이드",
                "VERIFIED-CODE", "VERIFIED-TYPE", "FACE", 2, 5,
                List.of("검증된 정상 증상"), List.of("검증된 경고 증상"),
                List.of("검증된 사후 제한"), List.of("ALLOWED"), List.of("RESTRICTED"),
                "WHS", "1.0.0");
    }
}
