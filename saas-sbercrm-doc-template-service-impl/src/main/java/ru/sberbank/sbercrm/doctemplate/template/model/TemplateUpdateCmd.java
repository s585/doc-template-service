package ru.sberbank.sbercrm.doctemplate.template.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.sberbank.sbercrm.doctemplate.template.model.rule.Rule;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TemplateUpdateCmd {
    private String name;
    private String description;
    private Rule displayCondition;
    private boolean active;
    @Builder.Default
    private List<TemplateMapping> mappings = new ArrayList<>();
}
