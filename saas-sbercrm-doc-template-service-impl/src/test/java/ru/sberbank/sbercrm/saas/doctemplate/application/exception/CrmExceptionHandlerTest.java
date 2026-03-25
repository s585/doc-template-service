package ru.sberbank.sbercrm.saas.doctemplate.application.exception;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Locale;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.context.support.StaticMessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import ru.sberbank.sbercrm.saas.doctemplate.application.exception.model.BusinessCrmException;
import ru.sberbank.sbercrm.saas.doctemplate.application.exception.model.NotFoundCrmException;
import ru.sberbank.sbercrm.saas.doctemplate.application.exception.model.SystemCrmException;

class CrmExceptionHandlerTest {
    private static final Locale LOCALE = Locale.forLanguageTag("ru-RU");

    @AfterEach
    void tearDown() {
        LocaleContextHolder.resetLocaleContext();
    }

    @Test
    @DisplayName("Обработчик возвращает 404 и локализованное сообщение для not found ошибки")
    void givenNotFoundException_whenHandleNotFound_thenReturnLocalizedResponse() {
        // given
        CrmExceptionHandler handler = createHandler();
        NotFoundCrmException exception = new NotFoundCrmException("template.not_found", "template-id");
        LocaleContextHolder.setLocale(LOCALE);

        // when
        ResponseEntity<Map<String, Object>> response = handler.handleNotFound(exception);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody())
            .containsEntry("code", "template.not_found")
            .containsEntry("message", "Шаблон не найден: template-id");
        assertThat((Object[]) response.getBody().get("params")).containsExactly("template-id");
    }

    @Test
    @DisplayName("Обработчик возвращает 400 и код ошибки для business исключения")
    void givenBusinessException_whenHandleBusiness_thenReturnBadRequestResponse() {
        // given
        CrmExceptionHandler handler = createHandler();
        BusinessCrmException exception = new BusinessCrmException("template.variable.pattern_invalid");
        LocaleContextHolder.setLocale(LOCALE);

        // when
        ResponseEntity<Map<String, Object>> response = handler.handleBusiness(exception);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody())
            .containsEntry("code", "template.variable.pattern_invalid")
            .containsEntry("message", "Некорректный regex для поиска переменных шаблона");
        assertThat((Object[]) response.getBody().get("params")).isEmpty();
    }

    @Test
    @DisplayName("Обработчик возвращает 500 и подставляет код как fallback сообщение")
    void givenSystemExceptionWithoutMessageTemplate_whenHandleSystem_thenReturnFallbackMessage() {
        // given
        CrmExceptionHandler handler = createHandler();
        SystemCrmException exception = new SystemCrmException("unknown.code", (Object[]) null);
        LocaleContextHolder.setLocale(LOCALE);

        // when
        ResponseEntity<Map<String, Object>> response = handler.handleSystem(exception);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody())
            .containsEntry("code", "unknown.code")
            .containsEntry("message", "unknown.code");
        assertThat((Object[]) response.getBody().get("params")).isEmpty();
    }

    private CrmExceptionHandler createHandler() {
        StaticMessageSource messageSource = new StaticMessageSource();
        messageSource.addMessage("template.not_found", LOCALE, "Шаблон не найден: {0}");
        messageSource.addMessage(
            "template.variable.pattern_invalid",
            LOCALE,
            "Некорректный regex для поиска переменных шаблона"
        );
        return new CrmExceptionHandler(messageSource);
    }
}
