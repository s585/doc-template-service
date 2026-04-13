package ru.sberbank.sbercrm.saas.doctemplate.document.usecase;

import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;
import ru.sberbank.sbercrm.saas.doctemplate.document.model.GenerationJob;
import ru.sberbank.sbercrm.saas.doctemplate.document.service.GenerationJobService;
import ru.sberbank.sbercrm.saas.doctemplate.document.service.GenerationJobTransitionService;
import ru.sberbank.sbercrm.saas.doctemplate.document.service.GenerationWorkerIdentityProvider;

@Slf4j
@Component
@RequiredArgsConstructor
public class GenerationJobDispatchUseCaseImpl implements GenerationJobDispatchUseCase {
    private final GenerationJobService generationJobService;
    private final GenerationJobTransitionService generationJobTransitionService;
    private final GenerationJobExecutionUseCase generationJobExecutionUseCase;
    private final ThreadPoolTaskExecutor generationJobTaskExecutor;
    private final GenerationWorkerIdentityProvider generationWorkerIdentityProvider;

    @Override
    public void dispatch() {
        int requeuedJobs = generationJobTransitionService.requeueTimedOutJobs(generationWorkerIdentityProvider.getWorkerId());
        if (requeuedJobs > 0) {
            log.warn(
                "Recovered timed out generation jobs: count={}, workerId={}, workerName={}",
                requeuedJobs,
                generationWorkerIdentityProvider.getWorkerId(),
                generationWorkerIdentityProvider.getWorkerName()
            );
        }

        int availableSlots = getAvailableSlots();
        if (availableSlots <= 0) {
            return;
        }

        List<GenerationJob> jobs = generationJobService.claimNextJobs(generationWorkerIdentityProvider.getWorkerId(), availableSlots);
        if (!jobs.isEmpty()) {
            log.info(
                "Claimed generation jobs: count={}, availableSlots={}, workerId={}, workerName={}, jobIds={}",
                jobs.size(),
                availableSlots,
                generationWorkerIdentityProvider.getWorkerId(),
                generationWorkerIdentityProvider.getWorkerName(),
                jobs.stream().map(GenerationJob::getId).toList()
            );
        }
        jobs.forEach(job -> generationJobTaskExecutor.execute(() -> generationJobExecutionUseCase.execute(job)));
    }

    private int getAvailableSlots() {
        int maxPoolSize = generationJobTaskExecutor.getMaxPoolSize();
        int activeCount = generationJobTaskExecutor.getActiveCount();
        return Math.max(maxPoolSize - activeCount, 0);
    }
}
