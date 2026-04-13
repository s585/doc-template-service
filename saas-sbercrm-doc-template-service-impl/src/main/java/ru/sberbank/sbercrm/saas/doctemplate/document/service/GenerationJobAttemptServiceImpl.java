package ru.sberbank.sbercrm.saas.doctemplate.document.service;

import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.sberbank.sbercrm.saas.doctemplate.document.model.GenerationJobAttempt;
import ru.sberbank.sbercrm.saas.doctemplate.document.repository.GenerationJobAttemptRepository;

@Service
@RequiredArgsConstructor
public class GenerationJobAttemptServiceImpl implements GenerationJobAttemptService {
    private final GenerationJobAttemptRepository generationJobAttemptRepository;

    @Override
    public GenerationJobAttempt create(UUID userId, UUID jobId, UUID workerId) {
        return generationJobAttemptRepository.create(userId, jobId, workerId);
    }

    @Override
    public List<GenerationJobAttempt> findByJobId(UUID jobId) {
        return generationJobAttemptRepository.findByJobId(jobId);
    }

    @Override
    public void markCompleted(UUID userId, UUID attemptId) {
        generationJobAttemptRepository.markCompleted(userId, attemptId);
    }

    @Override
    public void markFailed(UUID userId, UUID attemptId, String errorCode, String errorMessage) {
        generationJobAttemptRepository.markFailed(userId, attemptId, errorCode, errorMessage);
    }

    @Override
    public void markTimedOutActiveAttempt(UUID userId, UUID jobId) {
        generationJobAttemptRepository.markTimedOutActiveAttempt(userId, jobId);
    }
}
