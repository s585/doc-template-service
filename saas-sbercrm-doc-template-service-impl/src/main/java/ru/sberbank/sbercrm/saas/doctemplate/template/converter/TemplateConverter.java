package ru.sberbank.sbercrm.saas.doctemplate.template.converter;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import ru.sberbank.sbercrm.doctemplate.template.dto.TemplateCreationRq;
import ru.sberbank.sbercrm.doctemplate.template.dto.TemplateRs;
import ru.sberbank.sbercrm.doctemplate.template.dto.TemplateUpdateRq;
import ru.sberbank.sbercrm.saas.doctemplate.template.model.TemplateCreationCmd;
import ru.sberbank.sbercrm.saas.doctemplate.template.model.Template;
import ru.sberbank.sbercrm.saas.doctemplate.template.model.TemplateUpdateCmd;
import ru.sberbank.sbercrm.saas.doctemplate.template.model.TemplateFormat;

@Mapper(componentModel = "spring", uses = {TemplateMappingConverter.class})
public interface TemplateConverter {
    TemplateCreationCmd convertToModel(TemplateCreationRq request);

    TemplateUpdateCmd convertToModel(TemplateUpdateRq request);

    @Mapping(target = "format", source = "format", qualifiedByName = "convertFormatToValue")
    TemplateRs convertToRs(Template template);

    @Named("convertFormatToValue")
    default String convertFormatToValue(TemplateFormat format) {
        return format == null ? null : format.value();
    }
}
