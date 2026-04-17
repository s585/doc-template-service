package ru.sberbank.sbercrm.saas.doctemplate.document.model;

import java.util.LinkedHashMap;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class GenerationTemplateContext {
    @Builder.Default
    private Map<String, String> values = new LinkedHashMap<>();
    private String generatedFileName;
}
