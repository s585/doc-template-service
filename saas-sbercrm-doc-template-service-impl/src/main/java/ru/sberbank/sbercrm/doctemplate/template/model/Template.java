package ru.sberbank.sbercrm.doctemplate.template.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.sberbank.sbercrm.doctemplate.template.model.rule.Rule;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class Template {
    private UUID id;
    private UUID entityId;
    private String name;
    private String code;
    private String description;
    private TemplateFormat format;
    private String s3Key;
    private boolean active;
    private Rule displayCondition;
    private UUID createdBy;
    private UUID updatedBy;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
