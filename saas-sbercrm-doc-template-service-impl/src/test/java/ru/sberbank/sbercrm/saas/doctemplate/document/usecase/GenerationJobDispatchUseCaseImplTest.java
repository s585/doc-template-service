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
import ru.sberbank.sbercrm.saas.doctemplate.document.service.GenerationJobTransitionService;
import ru.sberbank.sbercrm.saas.doctemplate.document.service.GenerationWorkerIdentityProvider;

@ExtendWith(MockitoExtension.class)
class GenerationJobDispatchUseCaseImplTest {
    private static final UUID FIRST_JOB_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID SECOND_JOB_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID WORKER_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");

    @Mock
    private GenerationJobTransitionService generationJobTransitionService;

    @Mock
    private GenerationJobExecutionUseCase generationJobExecutionUseCase;

    @Mock
    private ThreadPoolTaskExecutor generationJobTaskExecutor;

    @Mock
    private GenerationWorkerIdentityProvider generationWorkerIdentityProvider;

    @InjectMocks
    private GenerationJobDispatchUseCaseImpl systemUnderTest;

    @Test
    @DisplayName("Диспетчер не запрашивает задачи, если свободных слотов воркера нет")
    void givenNoAvailableSlots_whenDispatch_thenSkipClaim() {
        given(generationWorkerIdentityProvider.getWorkerId()).willReturn(WORKER_ID);
        given(generationJobTaskExecutor.getMaxPoolSize()).willReturn(4);
        given(generationJobTaskExecutor.getActiveCount()).willReturn(4);

        systemUnderTest.dispatch();

        verify(generationJobTransitionService).requeueTimedOutJobs(WORKER_ID);
        verify(generationJobTransitionService, never()).claimNextJobsForProcessing(any(), any(Integer.class));
        verify(generationJobTaskExecutor, never()).execute(any(Runnable.class));
    }

    @Test
    @DisplayName("Диспетчер запрашивает задачи по доступной емкости и отправляет каждую в исполнитель")
    void givenAvailableSlots_whenDispatch_thenClaimAndSubmitJobs() {
        GenerationJob firstJob = GenerationJob.builder().id(FIRST_JOB_ID).build();
        GenerationJob secondJob = GenerationJob.builder().id(SECOND_JOB_ID).build();
        given(generationWorkerIdentityProvider.getWorkerId()).willReturn(WORKER_ID);
        given(generationWorkerIdentityProvider.getWorkerName()).willReturn("doc-template@host:123");
        given(generationJobTaskExecutor.getMaxPoolSize()).willReturn(4);
        given(generationJobTaskExecutor.getActiveCount()).willReturn(2);
        given(generationJobTransitionService.claimNextJobsForProcessing(WORKER_ID, 2))
            .willReturn(List.of(firstJob, secondJob));

        systemUnderTest.dispatch();

        verify(generationJobTransitionService).requeueTimedOutJobs(WORKER_ID);
        verify(generationJobTransitionService).claimNextJobsForProcessing(WORKER_ID, 2);
        verify(generationJobTaskExecutor, times(2)).execute(any(Runnable.class));
    }
}
