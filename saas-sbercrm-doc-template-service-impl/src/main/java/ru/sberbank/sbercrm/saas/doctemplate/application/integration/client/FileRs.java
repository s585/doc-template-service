package ru.sberbank.sbercrm.saas.doctemplate.application.integration.client;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class FileRs {
    private String key;
    private String path;
    private String source;
    private String fileName;
    private Long size;
    private UUID createdBy;
    private UUID updatedBy;
    private OffsetDateTime createdDate;
    private OffsetDateTime updatedDate;
}
