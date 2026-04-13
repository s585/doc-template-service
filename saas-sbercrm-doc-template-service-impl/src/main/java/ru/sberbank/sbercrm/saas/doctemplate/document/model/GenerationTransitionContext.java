package ru.sberbank.sbercrm.saas.doctemplate.document.model;

import java.util.UUID;
import lombok.Builder;

@Builder
public record GenerationTransitionContext(
    UUID tenantId,
    UUID userId,
    UUID attemptId,
    int attemptNo,
    UUID jobId,
    UUID documentId,
    String format
) {
}
