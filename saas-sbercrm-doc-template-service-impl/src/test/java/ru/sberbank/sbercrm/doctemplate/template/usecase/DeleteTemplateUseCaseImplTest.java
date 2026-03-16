package ru.sberbank.sbercrm.doctemplate.template.usecase;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.sberbank.sbercrm.doctemplate.common.constant.CrmErrorCodes;
import ru.sberbank.sbercrm.doctemplate.common.exception.NotFoundCrmException;
import ru.sberbank.sbercrm.doctemplate.template.adapter.filestorage.FileStorageAdapter;
import ru.sberbank.sbercrm.doctemplate.template.service.TemplateService;

@ExtendWith(MockitoExtension.class)
class DeleteTemplateUseCaseImplTest {
    private static final UUID TENANT_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID USER_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID TEMPLATE_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");

    @Mock
    private TemplateService templateService;

    @Mock
    private FileStorageAdapter fileStorageAdapter;

    @InjectMocks
    private DeleteTemplateUseCaseImpl deleteTemplateUseCase;

    @Test
    @DisplayName("Удаление шаблона выбрасывает 404, если шаблон не найден")
    void givenMissingTemplate_whenExecute_thenThrowNotFoundException() {
        // given
        given(templateService.findById(TENANT_ID, TEMPLATE_ID)).willReturn(Optional.empty());

        // expected
        assertThatThrownBy(() -> deleteTemplateUseCase.execute(TENANT_ID, USER_ID, TEMPLATE_ID))
            .isInstanceOf(NotFoundCrmException.class)
            .satisfies(ex -> org.assertj.core.api.Assertions.assertThat(((NotFoundCrmException) ex).getCode())
                .isEqualTo(CrmErrorCodes.TEMPLATE_NOT_FOUND));

        verify(templateService).findById(TENANT_ID, TEMPLATE_ID);
        verify(fileStorageAdapter, never()).deleteFile(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
        verify(templateService, never()).delete(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }
}
