package com.innerderma.skindiagnosis.application;

import com.innerderma.common.error.BusinessException;
import com.innerderma.common.error.ErrorCode;
import com.innerderma.skindiagnosis.domain.WhsSkinDiagnosis;
import com.innerderma.skindiagnosis.domain.WhsSkinDiagnosisRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class WhsSkinDiagnosisService {

    private final WhsSkinDiagnosisRepository diagnosisRepository;

    public WhsSkinDiagnosisService(WhsSkinDiagnosisRepository diagnosisRepository) {
        this.diagnosisRepository = diagnosisRepository;
    }

    public WhsSkinDiagnosis getLatestDiagnosis(String userCode) {
        return diagnosisRepository.findTopByUser_UserCodeOrderByDiagnosedDateDesc(userCode)
                .orElseThrow(() -> new BusinessException(ErrorCode.SKIN_DIAGNOSIS_NOT_FOUND));
    }
}
