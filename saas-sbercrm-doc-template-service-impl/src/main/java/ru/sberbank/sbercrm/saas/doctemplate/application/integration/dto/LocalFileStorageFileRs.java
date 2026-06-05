package ru.sberbank.sbercrm.saas.doctemplate.application.integration.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LocalFileStorageFileRs {
    private String key;
    private String rootPath;
    private String path;
    private boolean exists;
    private boolean regularFile;
    private Long sizeBytes;
}
