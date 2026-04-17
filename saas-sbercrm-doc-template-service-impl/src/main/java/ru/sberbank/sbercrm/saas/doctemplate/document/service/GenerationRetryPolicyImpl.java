package ru.sberbank.sbercrm.saas.doctemplate.document.service;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import ru.sberbank.sbercrm.saas.doctemplate.document.model.GenerationErrorDecision;
import ru.sberbank.sbercrm.saas.doctemplate.document.model.GenerationRetryAction;
import ru.sberbank.sbercrm.saas.doctemplate.document.model.GenerationRetryDecision;
import ru.sberbank.sbercrm.saas.doctemplate.template.properties.DocTemplateProperties;

@Service
public class GenerationRetryPolicyImpl implements GenerationRetryPolicy {
    private final Clock clock;
    private final DocTemplateProperties docTemplateProperties;

    public GenerationRetryPolicyImpl(Clock clock, DocTemplateProperties docTemplateProperties) {
        this.clock = clock;
        this.docTemplateProperties = docTemplateProperties;
    }

    @Override
    public GenerationRetryDecision decide(int attemptNo, GenerationErrorDecision errorDecision) {
        if (attemptNo <= 0) {
            throw new IllegalArgumentException("attemptNo must be >= 1");
        }
        if (!errorDecision.retriable()
            || attemptNo >= docTemplateProperties.getGeneration().getMaxAttempts()) {
            return new GenerationRetryDecision(
                GenerationRetryAction.FAIL_FINAL,
                errorDecision.errorCode(),
                errorDecision.errorMessage(),
                null
            );
        }

        List<Long> backoffSeconds = docTemplateProperties.getGeneration().getRetryBackoffSeconds();
        long seconds =
            backoffSeconds.isEmpty()
                ? 0L
                : backoffSeconds.get(Math.min(attemptNo - 1, backoffSeconds.size() - 1));
        OffsetDateTime now = OffsetDateTime.now(clock);
        OffsetDateTime nextRetryAt = now.plusSeconds(seconds);
        return new GenerationRetryDecision(
            nextRetryAt.isAfter(now) ? GenerationRetryAction.RETRY_LATER : GenerationRetryAction.RETRY_NOW,
            errorDecision.errorCode(),
            errorDecision.errorMessage(),
            nextRetryAt
        );
    }
}
