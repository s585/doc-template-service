package ru.sberbank.sbercrm.saas.doctemplate.template.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.sberbank.sbercrm.saas.doctemplate.shared.dto.FilterDto;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TemplateUpdateCmd {
    private String name;
    private String description;
    private FilterDto displayCondition;
    private boolean active;
    @Builder.Default
    private List<TemplateMapping> mappings = new ArrayList<>();
}
