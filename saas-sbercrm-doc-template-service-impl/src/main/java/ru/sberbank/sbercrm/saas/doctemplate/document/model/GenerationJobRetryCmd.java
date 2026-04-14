package ru.sberbank.sbercrm.saas.doctemplate.document.model;

import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class GenerationJobRetryCmd {
    UUID tenantId;
    UUID userId;
    UUID jobId;
    int expectedAttemptCount;
    int attemptCount;
    OffsetDateTime nextRetryAt;
    String errorCode;
    String errorMessage;
}
