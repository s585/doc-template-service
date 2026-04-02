package ru.sberbank.sbercrm.saas.doctemplate.document.model;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class Document {
    private UUID id;
    private UUID templateId;
    private UUID entityId;
    private UUID objectId;
    private UUID requestId;
    @Builder.Default
    private List<GeneratedFile> files = new ArrayList<>();
    private OffsetDateTime createdAt;
    private UUID createdBy;
    private OffsetDateTime updatedAt;
    private UUID updatedBy;
}
