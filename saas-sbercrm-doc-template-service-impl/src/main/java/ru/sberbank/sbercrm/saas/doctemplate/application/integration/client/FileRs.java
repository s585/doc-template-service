package ru.sberbank.sbercrm.saas.doctemplate.application.integration.client;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class FileRs {
    private String key;
    private String path;
    private String source;
    private String fileName;
}
