package ru.sberbank.sbercrm.saas.doctemplate.document.model;

import java.util.UUID;
import lombok.Builder;

/**
 * Контекст перехода, который используется до успешного создания {@code generation_job_attempt}.
 */
@Builder
public record GenerationJobPreAttemptContext(
    UUID tenantId,
    UUID userId,
    UUID jobId,
    UUID documentId,
    String format,
    int currentAttemptCount
) {
}
