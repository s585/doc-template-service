package ru.sberbank.sbercrm.saas.doctemplate.application.integration.client;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class FileFilterRq {
    private Map<String, List<String>> tags;
    private String source;
    private String originalFileName;
    private String prefixKey;
    private UUID createdBy;
    private UUID updatedBy;
    private List<String> fileTypes;
}
