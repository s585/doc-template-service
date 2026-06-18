package ru.sberbank.sbercrm.saas.doctemplate.template.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class TemplateVariableInfo {
    private String key;
    private MappingScope scope;
    private String blockId;
}
