package ru.sberbank.sbercrm.doctemplate.template.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.sberbank.sbercrm.doctemplate.template.model.MappingScope;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TemplateVariableInfo {
    private String key;
    private MappingScope scope;
}
