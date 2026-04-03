package ru.sberbank.sbercrm.saas.doctemplate.document.usecase;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import ru.sberbank.sbercrm.saas.doctemplate.document.model.GenerationJob;
import ru.sberbank.sbercrm.saas.doctemplate.document.service.GenerationJobService;

@ExtendWith(MockitoExtension.class)
class GenerationJobDispatchUseCaseImplTest {
    private static final UUID FIRST_JOB_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID SECOND_JOB_ID = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");

    @Mock
    private GenerationJobService generationJobService;

    @Mock
    private GenerationJobExecutionUseCase generationJobExecutionUseCase;

    @Mock
    private ThreadPoolTaskExecutor generationJobTaskExecutor;

    @InjectMocks
    private GenerationJobDispatchUseCaseImpl systemUnderTest;

    @Test
    @DisplayName("Dispatch не запрашивает job, если свободных worker slots нет")
    void givenNoAvailableSlots_whenDispatch_thenSkipClaim() {
        given(generationJobTaskExecutor.getMaxPoolSize()).willReturn(4);
        given(generationJobTaskExecutor.getActiveCount()).willReturn(4);

        systemUnderTest.dispatch();

        verify(generationJobService, never()).claimNextJobs(any(), any(Integer.class));
        verify(generationJobTaskExecutor, never()).execute(any(Runnable.class));
    }

    @Test
    @DisplayName("Dispatch запрашивает job по capacity и отправляет каждую в executor")
    void givenAvailableSlots_whenDispatch_thenClaimAndSubmitJobs() {
        GenerationJob firstJob = GenerationJob.builder().id(FIRST_JOB_ID).build();
        GenerationJob secondJob = GenerationJob.builder().id(SECOND_JOB_ID).build();
        given(generationJobTaskExecutor.getMaxPoolSize()).willReturn(4);
        given(generationJobTaskExecutor.getActiveCount()).willReturn(2);
        given(generationJobService.claimNextJobs(any(), eq(2))).willReturn(List.of(firstJob, secondJob));

        systemUnderTest.dispatch();

        verify(generationJobService).claimNextJobs(any(), eq(2));
        verify(generationJobTaskExecutor, times(2)).execute(any(Runnable.class));
    }
}
