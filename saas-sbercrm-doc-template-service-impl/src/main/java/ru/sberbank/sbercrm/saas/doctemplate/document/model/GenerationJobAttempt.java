package ru.sberbank.sbercrm.saas.doctemplate.document.model;

import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.checkerframework.checker.nullness.qual.Nullable;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class GenerationJobAttempt {
    private UUID id;
    private UUID jobId;
    private Integer attemptNo;
    @Nullable
    private UUID workerId;
    private OffsetDateTime startedAt;
    @Nullable
    private OffsetDateTime finishedAt;
    private String status;
    @Nullable
    private String errorCode;
    @Nullable
    private String errorMessage;
    @Nullable
    private UUID createdBy;
    @Nullable
    private UUID updatedBy;
}
