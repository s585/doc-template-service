package ru.sberbank.sbercrm.saas.doctemplate.document.model;

import java.util.UUID;
import java.time.OffsetDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.checkerframework.checker.nullness.qual.Nullable;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class GenerationJob {
    private UUID id;
    private UUID tenantId;
    private UUID documentId;
    private UUID templateId;
    private UUID entityId;
    private UUID objectId;
    private String format;
    private String status;
    @Nullable
    private String errorCode;
    @Nullable
    private String errorMessage;
    private OffsetDateTime createdAt;
    private UUID createdBy;
    private OffsetDateTime updatedAt;
    private UUID updatedBy;
}
