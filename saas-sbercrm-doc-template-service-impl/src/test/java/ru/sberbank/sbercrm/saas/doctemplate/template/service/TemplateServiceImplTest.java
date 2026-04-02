package ru.sberbank.sbercrm.saas.doctemplate.template.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.sberbank.sbercrm.saas.doctemplate.template.model.Template;
import ru.sberbank.sbercrm.saas.doctemplate.template.repository.TemplateMappingRepository;
import ru.sberbank.sbercrm.saas.doctemplate.template.repository.TemplateRepository;

@ExtendWith(MockitoExtension.class)
class TemplateServiceImplTest {
    private static final UUID TENANT_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID TEMPLATE_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");

    @Mock
    private TemplateRepository templateRepository;

    @Mock
    private TemplateMappingRepository templateMappingRepository;

    @InjectMocks
    private TemplateServiceImpl templateService;

    @Test
    @DisplayName("findById использует агрегирующий метод и возвращает шаблон из репозитория")
    void whenFindById_thenDelegateToAggregateLookup() {
        // given
        Template template = Template.builder().id(TEMPLATE_ID).build();
        given(templateRepository.findById(TENANT_ID, TEMPLATE_ID)).willReturn(Optional.of(template));

        // when
        Optional<Template> result = templateService.findById(TENANT_ID, TEMPLATE_ID);

        // then
        assertThat(result).contains(template);
        verify(templateRepository).findById(TENANT_ID, TEMPLATE_ID);
    }

    @Test
    @DisplayName("exists делегирует cheap existence check в репозиторий")
    void whenExists_thenDelegateToRepository() {
        // given
        given(templateRepository.exists(TENANT_ID, TEMPLATE_ID)).willReturn(true);

        // when
        boolean result = templateService.exists(TENANT_ID, TEMPLATE_ID);

        // then
        assertThat(result).isTrue();
        verify(templateRepository).exists(TENANT_ID, TEMPLATE_ID);
    }
}
