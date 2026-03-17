package ru.sberbank.sbercrm.doctemplate.template.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.doThrow;

import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import ru.sberbank.sbercrm.doctemplate.common.constant.CrmErrorCodes;
import ru.sberbank.sbercrm.doctemplate.common.exception.BusinessCrmException;
import ru.sberbank.sbercrm.doctemplate.template.adapter.filestorage.FileRs;
import ru.sberbank.sbercrm.doctemplate.template.adapter.filestorage.FileStorageAdapter;
import ru.sberbank.sbercrm.doctemplate.template.config.TemplateProperties;
import ru.sberbank.sbercrm.doctemplate.template.model.MappingScope;
import ru.sberbank.sbercrm.doctemplate.template.model.Template;
import ru.sberbank.sbercrm.doctemplate.template.model.TemplateCreationCmd;
import ru.sberbank.sbercrm.doctemplate.template.model.TemplateFormat;
import ru.sberbank.sbercrm.doctemplate.template.model.TemplateVariableInfo;
import ru.sberbank.sbercrm.doctemplate.template.processor.TemplateProcessingFacade;
import ru.sberbank.sbercrm.doctemplate.template.service.TemplateService;

@ExtendWith(MockitoExtension.class)
class ImportTemplateUseCaseImplTest {
    private static final UUID TENANT_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID USER_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID ENTITY_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
    @Mock
    private TemplateService templateService;

    @Mock
    private FileStorageAdapter fileStorageAdapter;

    @Mock
    private TemplateProcessingFacade templateProcessingFacade;

    @InjectMocks
    private ImportTemplateUseCaseImpl importTemplateUseCase;

    @Test
    @DisplayName("Импорт выбрасывает ошибку, если одна переменная найдена с разными scope")
    void givenVariableWithDifferentScopes_whenExecute_thenThrowBusinessException() {
        // given
        TemplateProperties templateProperties = new TemplateProperties();
        templateProperties.getFileStorage().setFolder("/doc-template");
        importTemplateUseCase = new ImportTemplateUseCaseImpl(
            templateService,
            fileStorageAdapter,
            templateProperties,
            templateProcessingFacade
        );
        TemplateCreationCmd command = TemplateCreationCmd.builder()
            .entityId(ENTITY_ID)
            .name("Договор")
            .description("Описание")
            .code("CONTRACT")
            .build();
        MockMultipartFile file = new MockMultipartFile(
            "file",
            "template.docx",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            new byte[] {1, 2, 3}
        );
        given(templateProcessingFacade.extractVariables(eq(TemplateFormat.DOCX), any()))
            .willReturn(
                List.of(
                    TemplateVariableInfo.builder().key("deal_number").scope(MappingScope.VALUE).build(),
                    TemplateVariableInfo.builder().key("deal_number").scope(MappingScope.TABLE).build()
                )
            );

        // expected
        assertThatThrownBy(() -> importTemplateUseCase.execute(TENANT_ID, USER_ID, command, file))
            .isInstanceOf(BusinessCrmException.class)
            .satisfies(ex -> {
                BusinessCrmException exception = (BusinessCrmException) ex;
                assertThat(exception.getCode()).isEqualTo(CrmErrorCodes.TEMPLATE_VARIABLE_INVALID);
                assertThat(exception.getParams()).containsExactly("deal_number");
            });

        verify(templateService).checkCodeUnique(TENANT_ID, "CONTRACT", null);
        verify(fileStorageAdapter, never()).ensureFolderExists(any(), any(), any());
        verify(fileStorageAdapter, never()).upload(any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("Импорт удаляет файл из file storage, если создание шаблона завершается ошибкой")
    void givenUploadedFileAndCreateFailure_whenExecute_thenDeleteUploadedFile() {
        // given
        TemplateProperties templateProperties = new TemplateProperties();
        templateProperties.getFileStorage().setFolder("/doc-template");
        importTemplateUseCase = new ImportTemplateUseCaseImpl(
            templateService,
            fileStorageAdapter,
            templateProperties,
            templateProcessingFacade
        );
        String templateName = "Договор";
        String templateCode = "CONTRACT";
        String originalFileName = "template.docx";
        String uploadedKey = "templates/template.docx";
        TemplateCreationCmd command = TemplateCreationCmd.builder()
            .entityId(ENTITY_ID)
            .name(templateName)
            .description("Описание")
            .code(templateCode)
            .build();
        MockMultipartFile file = new MockMultipartFile(
            "file",
            originalFileName,
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            new byte[] {1, 2, 3}
        );
        given(templateProcessingFacade.extractVariables(eq(TemplateFormat.DOCX), any()))
            .willReturn(List.of(TemplateVariableInfo.builder().key("deal_number").scope(MappingScope.VALUE).build()));
        given(fileStorageAdapter.upload(any(), any(), any(), any(), any(), any()))
            .willReturn(FileRs.builder().key(uploadedKey).build());
        given(templateService.create(eq(TENANT_ID), any()))
            .willThrow(new RuntimeException("db error"));

        // expected
        assertThatThrownBy(() -> importTemplateUseCase.execute(TENANT_ID, USER_ID, command, file))
            .isInstanceOf(RuntimeException.class)
            .hasMessage("db error");

        verify(templateService, never()).createMappings(any(), any(), any(), any());
        verify(fileStorageAdapter).deleteFile(TENANT_ID, USER_ID, uploadedKey);
    }

    @Test
    @DisplayName("Импорт не скрывает основную ошибку, если удаление загруженного файла тоже завершилось ошибкой")
    void givenCleanupFailure_whenExecute_thenPropagatePrimaryException() {
        // given
        TemplateProperties templateProperties = new TemplateProperties();
        templateProperties.getFileStorage().setFolder("/doc-template");
        importTemplateUseCase = new ImportTemplateUseCaseImpl(
            templateService,
            fileStorageAdapter,
            templateProperties,
            templateProcessingFacade
        );
        String uploadedKey = "templates/template.docx";
        TemplateCreationCmd command = TemplateCreationCmd.builder()
            .entityId(ENTITY_ID)
            .name("Договор")
            .description("Описание")
            .code("CONTRACT")
            .build();
        MockMultipartFile file = new MockMultipartFile(
            "file",
            "template.docx",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            new byte[] {1, 2, 3}
        );
        given(templateProcessingFacade.extractVariables(eq(TemplateFormat.DOCX), any()))
            .willReturn(List.of());
        given(fileStorageAdapter.upload(any(), any(), any(), any(), any(), any()))
            .willReturn(FileRs.builder().key(uploadedKey).build());
        given(templateService.create(eq(TENANT_ID), any()))
            .willThrow(new RuntimeException("db error"));
        doThrow(new RuntimeException("cleanup error"))
            .when(fileStorageAdapter).deleteFile(TENANT_ID, USER_ID, uploadedKey);

        // expected
        assertThatThrownBy(() -> importTemplateUseCase.execute(TENANT_ID, USER_ID, command, file))
            .isInstanceOf(RuntimeException.class)
            .hasMessage("db error");
    }
}
