package com.innerderma.carecompletion.application;

import com.innerderma.carecompletion.domain.*;
import com.innerderma.carehistory.application.CarePhase;
import com.innerderma.caresolution.application.*;
import com.innerderma.caresolution.domain.CareSolution;
import com.innerderma.user.domain.*;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class CareCompletionServiceTest {
    @Test
    void morningCompletionUsesSolutionAvailableBeforeThatDay() {
        UserRepository userRepository = mock(UserRepository.class);
        CareCompletionRepository repository = mock(CareCompletionRepository.class);
        CareSolutionService solutionService = mock(CareSolutionService.class);
        CareCompletionService service = new CareCompletionService(userRepository, repository, solutionService);
        User user = new User("WHS-DEMO-001", "테스트 사용자", "010-1234-1234");
        LocalDate date = LocalDate.of(2026, 8, 18);
        CareSolutionResult result = mock(CareSolutionResult.class);
        CareSolution solution = mock(CareSolution.class);
        when(result.solution()).thenReturn(solution);
        when(userRepository.findByUserCode("WHS-DEMO-001")).thenReturn(Optional.of(user));
        when(solutionService.getDaily("WHS-DEMO-001", date.minusDays(1))).thenReturn(result);
        when(repository.findByUser_UserCodeAndServedDateAndPhase(
                "WHS-DEMO-001", date, CarePhase.MORNING)).thenReturn(Optional.empty());
        when(repository.save(any(CareCompletion.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CareCompletion saved = service.save("WHS-DEMO-001", date, CarePhase.MORNING, true);

        assertThat(saved.getServedDate()).isEqualTo(date);
        assertThat(saved.getPhase()).isEqualTo(CarePhase.MORNING);
        assertThat(saved.isCompleted()).isTrue();
        assertThat(saved.getCareSolution()).isSameAs(solution);
    }

    @Test
    void savingAgainUpdatesExistingCompletionInsteadOfDuplicatingIt() {
        UserRepository userRepository = mock(UserRepository.class);
        CareCompletionRepository repository = mock(CareCompletionRepository.class);
        CareSolutionService solutionService = mock(CareSolutionService.class);
        CareCompletionService service = new CareCompletionService(userRepository, repository, solutionService);
        LocalDate date = LocalDate.of(2026, 8, 18);
        CareCompletion existing = mock(CareCompletion.class);
        CareSolutionResult result = mock(CareSolutionResult.class);
        CareSolution solution = mock(CareSolution.class);
        when(result.solution()).thenReturn(solution);
        when(userRepository.findByUserCode("WHS-DEMO-001"))
                .thenReturn(Optional.of(new User("WHS-DEMO-001", "테스트", "010")));
        when(solutionService.getDaily("WHS-DEMO-001", date)).thenReturn(result);
        when(repository.findByUser_UserCodeAndServedDateAndPhase(
                "WHS-DEMO-001", date, CarePhase.EVENING)).thenReturn(Optional.of(existing));
        when(repository.save(existing)).thenReturn(existing);

        assertThat(service.save("WHS-DEMO-001", date, CarePhase.EVENING, false)).isSameAs(existing);
        verify(existing).update(eq(solution), eq(false), any());
        verify(repository).save(existing);
    }

    @Test
    void rejectsCompletionHistoryLongerThanThirtyOneDays() {
        UserRepository userRepository = mock(UserRepository.class);
        CareCompletionRepository repository = mock(CareCompletionRepository.class);
        CareCompletionService service = new CareCompletionService(userRepository, repository,
                mock(CareSolutionService.class));
        when(userRepository.existsByUserCode("WHS-DEMO-001")).thenReturn(true);

        assertThatThrownBy(() -> service.getHistory("WHS-DEMO-001",
                LocalDate.of(2026, 7, 1), LocalDate.of(2026, 8, 17)))
                .isInstanceOf(com.innerderma.common.error.BusinessException.class);
        verifyNoInteractions(repository);
    }
}
