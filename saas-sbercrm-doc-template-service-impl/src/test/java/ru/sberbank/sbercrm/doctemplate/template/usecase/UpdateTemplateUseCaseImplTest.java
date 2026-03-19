package ru.sberbank.sbercrm.doctemplate.template.usecase;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.sberbank.sbercrm.saas.doctemplate.application.exception.CrmErrorCodes;
import ru.sberbank.sbercrm.saas.doctemplate.template.constant.TemplateConstants;
import ru.sberbank.sbercrm.saas.doctemplate.application.exception.model.NotFoundCrmException;
import ru.sberbank.sbercrm.saas.doctemplate.template.model.TemplateUpdateCmd;
import ru.sberbank.sbercrm.saas.doctemplate.template.service.TemplateService;
import ru.sberbank.sbercrm.saas.doctemplate.template.usecase.UpdateTemplateUseCaseImpl;

@ExtendWith(MockitoExtension.class)
class UpdateTemplateUseCaseImplTest {
    private static final UUID TENANT_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID USER_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID TEMPLATE_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");

    @Mock
    private TemplateService templateService;

    @InjectMocks
    private UpdateTemplateUseCaseImpl updateTemplateUseCase;

    @Test
    @DisplayName("Обновление шаблона выбрасывает 404, если шаблон не найден")
    void givenMissingTemplate_whenExecute_thenThrowNotFoundException() {
        // given
        TemplateUpdateCmd request = TemplateUpdateCmd.builder()
            .name("Обновленный шаблон")
            .active(true)
            .build();
        given(templateService.findById(TENANT_ID, TEMPLATE_ID)).willReturn(java.util.Optional.empty());

        // expected
        assertThatThrownBy(() -> updateTemplateUseCase.execute(TENANT_ID, USER_ID, TEMPLATE_ID, request))
            .isInstanceOf(NotFoundCrmException.class)
            .satisfies(ex -> org.assertj.core.api.Assertions.assertThat(((NotFoundCrmException) ex).getCode())
                .isEqualTo(TemplateConstants.ErrorCodes.TEMPLATE_NOT_FOUND));

        verify(templateService).findById(TENANT_ID, TEMPLATE_ID);
        verify(templateService, never()).update(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }
}
