package ru.sberbank.sbercrm.saas.doctemplate.document.model;

import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class GeneratedDocument {
    private UUID id;
    private UUID templateId;
    private UUID entityId;
    private UUID objectId;
    private UUID requestId;
    private OffsetDateTime createdAt;
    private UUID createdBy;
    private OffsetDateTime updatedAt;
    private UUID updatedBy;
}
