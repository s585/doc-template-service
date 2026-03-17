package ru.sberbank.sbercrm.doctemplate.template.converter;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import ru.sberbank.sbercrm.doctemplate.template.TemplateCreationRq;
import ru.sberbank.sbercrm.doctemplate.template.TemplateRs;
import ru.sberbank.sbercrm.doctemplate.template.TemplateUpdateRq;
import ru.sberbank.sbercrm.doctemplate.template.model.TemplateCreationCmd;
import ru.sberbank.sbercrm.doctemplate.template.model.Template;
import ru.sberbank.sbercrm.doctemplate.template.model.TemplateUpdateCmd;

@Mapper(componentModel = "spring", uses = {TemplateMappingConverter.class, RuleConverter.class})
public interface TemplateConverter {
    TemplateCreationCmd convertToModel(TemplateCreationRq request);

    TemplateUpdateCmd convertToModel(TemplateUpdateRq request);

    @Mapping(target = "format", source = "format", qualifiedByName = "convertFormatToValue")
    TemplateRs convertToRs(Template template);

    @Named("convertFormatToValue")
    default String convertFormatToValue(ru.sberbank.sbercrm.doctemplate.template.model.TemplateFormat format) {
        return format == null ? null : format.value();
    }
}
