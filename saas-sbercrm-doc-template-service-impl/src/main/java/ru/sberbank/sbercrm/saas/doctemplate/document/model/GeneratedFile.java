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
public class GeneratedFile {
    private UUID id;
    private UUID documentId;
    private String format;
    private String status;
    @Nullable
    private String s3Key;
    @Nullable
    private String checksum;
    @Nullable
    private Long sizeBytes;
    @Nullable
    private String errorCode;
    @Nullable
    private String errorMessage;
    private OffsetDateTime createdAt;
    private UUID createdBy;
    private OffsetDateTime updatedAt;
    private UUID updatedBy;
}
