package ru.sberbank.sbercrm.saas.doctemplate.document.usecase;

import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;
import ru.sberbank.sbercrm.saas.doctemplate.document.model.GenerationJob;
import ru.sberbank.sbercrm.saas.doctemplate.document.service.GenerationJobService;

@Component
@RequiredArgsConstructor
public class GenerationJobDispatchUseCaseImpl implements GenerationJobDispatchUseCase {
    private final GenerationJobService generationJobService;
    private final GenerationJobExecutionUseCase generationJobExecutionUseCase;
    private final ThreadPoolTaskExecutor generationJobTaskExecutor;
    private final UUID workerId = UUID.randomUUID();

    @Override
    public void dispatch() {
        int availableSlots = getAvailableSlots();
        if (availableSlots <= 0) {
            return;
        }

        List<GenerationJob> jobs = generationJobService.claimNextJobs(workerId, availableSlots);
        jobs.forEach(job -> generationJobTaskExecutor.execute(() -> generationJobExecutionUseCase.execute(job)));
    }

    private int getAvailableSlots() {
        int maxPoolSize = generationJobTaskExecutor.getMaxPoolSize();
        int activeCount = generationJobTaskExecutor.getActiveCount();
        return Math.max(maxPoolSize - activeCount, 0);
    }
}
