package ru.sberbank.sbercrm.saas.doctemplate.template.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TemplateCreationCmd {
    private UUID entityId;
    private String name;
    private String description;
    private String code;
}
