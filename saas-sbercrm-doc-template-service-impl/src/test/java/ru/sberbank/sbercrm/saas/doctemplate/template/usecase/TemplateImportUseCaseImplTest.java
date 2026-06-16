package ru.sberbank.sbercrm.saas.doctemplate.template.usecase;

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
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import ru.sberbank.sbercrm.saas.doctemplate.application.exception.CrmErrorCodes;
import ru.sberbank.sbercrm.saas.doctemplate.template.constant.TemplateConstants;
import ru.sberbank.sbercrm.saas.doctemplate.application.exception.model.BusinessCrmException;
import ru.sberbank.sbercrm.saas.doctemplate.application.exception.model.SystemCrmException;
import ru.sberbank.sbercrm.saas.doctemplate.application.integration.client.FileRs;
import ru.sberbank.sbercrm.saas.doctemplate.application.integration.gateway.FileStorageGateway;
import ru.sberbank.sbercrm.saas.doctemplate.application.integration.gateway.FileStoragePathResolver;
import ru.sberbank.sbercrm.saas.doctemplate.template.model.MappingScope;
import ru.sberbank.sbercrm.saas.doctemplate.template.model.Template;
import ru.sberbank.sbercrm.saas.doctemplate.template.model.TemplateCreationCmd;
import ru.sberbank.sbercrm.saas.doctemplate.template.model.TemplateFormat;
import ru.sberbank.sbercrm.saas.doctemplate.template.model.TemplateMapping;
import ru.sberbank.sbercrm.saas.doctemplate.template.model.TemplateVariableInfo;
import ru.sberbank.sbercrm.saas.doctemplate.template.processor.TemplateProcessingFacade;
import ru.sberbank.sbercrm.saas.doctemplate.template.service.TemplateService;

@ExtendWith(MockitoExtension.class)
class TemplateImportUseCaseImplTest {
    private static final UUID TENANT_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID USER_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID ENTITY_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
    @Mock
    private TemplateService templateService;

    @Mock
    private FileStorageGateway fileStorageGateway;

    @Mock
    private FileStoragePathResolver fileStoragePathResolver;

    @Mock
    private TemplateProcessingFacade templateProcessingFacade;

    @InjectMocks
    private TemplateImportUseCaseImpl systemUnderTest;

    @Test
    @DisplayName("Импорт допускает повтор placeholder-а в документе и создает один mapping на ключ")
    void givenRepeatedVariableInDocument_whenExecute_thenCreateSingleMappingForKey() {
        UUID templateId = UUID.fromString("55555555-5555-5555-5555-555555555555");
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
            .willReturn(List.of(
                TemplateVariableInfo.builder().key("client_name").scope(MappingScope.VALUE).build(),
                TemplateVariableInfo.builder().key("client_name").scope(MappingScope.VALUE).build()
            ));
        given(fileStoragePathResolver.templateFolder(ENTITY_ID)).willReturn("/doc-template/" + ENTITY_ID);
        given(fileStorageGateway.upload(any(), any(), any(), any(), any()))
            .willReturn(FileRs.builder().key("templates/template.docx").build());
        given(templateService.create(eq(TENANT_ID), any()))
            .willReturn(Template.builder().id(templateId).build());
        given(templateService.getMappings(TENANT_ID, templateId)).willReturn(List.of());

        systemUnderTest.execute(TENANT_ID, USER_ID, command, file);

        ArgumentCaptor<List<TemplateMapping>> mappingsCaptor = ArgumentCaptor.captor();
        verify(templateService).createMappings(eq(TENANT_ID), eq(templateId), eq(USER_ID), mappingsCaptor.capture());
        assertThat(mappingsCaptor.getValue())
            .extracting(TemplateMapping::getKey)
            .containsOnlyOnce("client_name");
    }

    @Test
    @DisplayName("Импорт выбрасывает ошибку, если одна переменная найдена с разными scope")
    void givenVariableWithDifferentScopes_whenExecute_thenThrowBusinessException() {
        // given
        systemUnderTest = new TemplateImportUseCaseImpl(
            templateService,
            fileStorageGateway,
            fileStoragePathResolver,
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
                    TemplateVariableInfo.builder().key("deal_number").scope(MappingScope.COLLECTION).build()
                )
            );

        // expected
        assertThatThrownBy(() -> systemUnderTest.execute(TENANT_ID, USER_ID, command, file))
            .isInstanceOf(BusinessCrmException.class)
            .satisfies(ex -> {
                BusinessCrmException exception = (BusinessCrmException) ex;
                assertThat(exception.getCode()).isEqualTo(TemplateConstants.ErrorCodes.TEMPLATE_VARIABLE_INVALID);
                assertThat(exception.getParams()).containsExactly("deal_number");
            });

        verify(templateService).checkCodeUnique(TENANT_ID, "CONTRACT", null);
        verify(fileStorageGateway, never()).upload(any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("Импорт удаляет файл из file storage, если создание шаблона завершается ошибкой")
    void givenUploadedFileAndCreateFailure_whenExecute_thenDeleteUploadedFile() {
        // given
        systemUnderTest = new TemplateImportUseCaseImpl(
            templateService,
            fileStorageGateway,
            fileStoragePathResolver,
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
        given(fileStoragePathResolver.templateFolder(ENTITY_ID)).willReturn("/doc-template/" + ENTITY_ID);
        given(fileStorageGateway.upload(any(), any(), any(), any(), any()))
            .willReturn(FileRs.builder().key(uploadedKey).build());
        given(templateService.create(eq(TENANT_ID), any()))
            .willThrow(new RuntimeException("db error"));

        // expected
        assertThatThrownBy(() -> systemUnderTest.execute(TENANT_ID, USER_ID, command, file))
            .isInstanceOf(SystemCrmException.class)
            .satisfies(ex -> {
                SystemCrmException exception = (SystemCrmException) ex;
                assertThat(exception.getCode()).isEqualTo(CrmErrorCodes.SYSTEM_UNEXPECTED);
                assertThat(exception.getParams()).containsExactly("RuntimeException");
                assertThat(exception.getCause()).hasMessage("db error");
            });

        verify(templateService, never()).createMappings(any(), any(), any(), any());
        verify(fileStorageGateway).deleteFile(TENANT_ID, USER_ID, uploadedKey);
    }

    @Test
    @DisplayName("Импорт пробрасывает ошибку rollback, если удаление загруженного файла завершается runtime-ошибкой")
    void givenCleanupFailure_whenExecute_thenPropagatePrimaryException() {
        // given
        systemUnderTest = new TemplateImportUseCaseImpl(
            templateService,
            fileStorageGateway,
            fileStoragePathResolver,
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
        given(fileStoragePathResolver.templateFolder(ENTITY_ID)).willReturn("/doc-template/" + ENTITY_ID);
        given(fileStorageGateway.upload(any(), any(), any(), any(), any()))
            .willReturn(FileRs.builder().key(uploadedKey).build());
        given(templateService.create(eq(TENANT_ID), any()))
            .willThrow(new RuntimeException("db error"));
        doThrow(new RuntimeException("cleanup error"))
            .when(fileStorageGateway).deleteFile(TENANT_ID, USER_ID, uploadedKey);

        // expected
        assertThatThrownBy(() -> systemUnderTest.execute(TENANT_ID, USER_ID, command, file))
            .isInstanceOf(RuntimeException.class)
            .hasMessage("cleanup error");
    }
}
