package ru.sberbank.sbercrm.doctemplate.template.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import java.io.IOException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;
import ru.sberbank.sbercrm.saas.doctemplate.application.exception.CrmErrorCodes;
import ru.sberbank.sbercrm.saas.doctemplate.template.constant.TemplateConstants;
import ru.sberbank.sbercrm.saas.doctemplate.application.exception.model.BusinessCrmException;
import ru.sberbank.sbercrm.saas.doctemplate.template.model.TemplateFormat;
import ru.sberbank.sbercrm.saas.doctemplate.template.util.TemplateFileUtils;

@ExtendWith(MockitoExtension.class)
class TemplateFileUtilsTest {

    @Mock
    private MultipartFile multipartFile;

    @ParameterizedTest
    @CsvSource({
        "template.docx, DOCX",
        "template.xlsx, XLSX"
    })
    @DisplayName("Определение формата возвращает ожидаемый формат для поддерживаемого расширения")
    void givenSupportedExtension_whenResolveFormat_thenReturnTemplateFormat(String fileName, TemplateFormat expectedFormat) {
        // given
        
        // when
        TemplateFormat actualFormat = TemplateFileUtils.resolveFormat(fileName);

        // then
        assertThat(actualFormat).isEqualTo(expectedFormat);
    }

    @Test
    @DisplayName("Определение формата выбрасывает ошибку для неподдерживаемого расширения")
    void givenUnsupportedExtension_whenResolveFormat_thenThrowBusinessException() {
        // given
        String fileName = "template.pdf";

        // expected
        assertThatThrownBy(() -> TemplateFileUtils.resolveFormat(fileName))
            .isInstanceOf(BusinessCrmException.class)
            .satisfies(ex -> {
                BusinessCrmException exception = (BusinessCrmException) ex;
                assertThat(exception.getCode()).isEqualTo(TemplateConstants.ErrorCodes.TEMPLATE_FORMAT_UNSUPPORTED);
                assertThat(exception.getParams()).containsExactly("pdf");
            });
    }

    @Test
    @DisplayName("Определение имени файла выбрасывает ошибку для пустого original filename")
    void givenMissingOriginalFileName_whenResolveOriginalFileName_thenThrowBusinessException() {
        // given
        given(multipartFile.getOriginalFilename()).willReturn(" ");

        // expected
        assertThatThrownBy(() -> TemplateFileUtils.resolveOriginalFileName(multipartFile))
            .isInstanceOf(BusinessCrmException.class)
            .satisfies(ex -> assertThat(((BusinessCrmException) ex).getCode()).isEqualTo(TemplateConstants.ErrorCodes.TEMPLATE_FILE_INVALID));
    }

    @Test
    @DisplayName("Чтение байтов выбрасывает ошибку при IOException")
    void givenMultipartFileReadFailure_whenReadBytes_thenThrowBusinessException() throws IOException {
        // given
        given(multipartFile.getBytes()).willThrow(new IOException("read failed"));

        // expected
        assertThatThrownBy(() -> TemplateFileUtils.readBytes(multipartFile))
            .isInstanceOf(BusinessCrmException.class)
            .satisfies(ex -> assertThat(((BusinessCrmException) ex).getCode()).isEqualTo(TemplateConstants.ErrorCodes.TEMPLATE_FILE_INVALID));
    }
}
