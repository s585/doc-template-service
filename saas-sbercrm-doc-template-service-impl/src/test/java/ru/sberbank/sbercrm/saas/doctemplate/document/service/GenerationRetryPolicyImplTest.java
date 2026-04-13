package ru.sberbank.sbercrm.saas.doctemplate.document.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ru.sberbank.sbercrm.saas.doctemplate.document.model.GenerationErrorDecision;
import ru.sberbank.sbercrm.saas.doctemplate.document.model.GenerationRetryAction;
import ru.sberbank.sbercrm.saas.doctemplate.template.properties.DocTemplateProperties;

class GenerationRetryPolicyImplTest {
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-04-03T12:00:00Z"), ZoneOffset.UTC);

    private final DocTemplateProperties docTemplateProperties = new DocTemplateProperties();
    private final GenerationRetryPolicyImpl systemUnderTest =
        new GenerationRetryPolicyImpl(CLOCK, docTemplateProperties);

    @Test
    @DisplayName("Retry policy возвращает RETRY_LATER для retriable ошибки в пределах лимита попыток")
    void givenRetriableErrorWithinLimit_whenDecide_thenReturnRetryLater() {
        docTemplateProperties.getGeneration().setMaxAttempts(4);
        docTemplateProperties.getGeneration().setRetryBackoffSeconds(java.util.List.of(10L, 30L));

        var decision = systemUnderTest.decide(
            1,
            new GenerationErrorDecision("file_storage.request_failed", "temporary", true)
        );

        assertThat(decision.action()).isEqualTo(GenerationRetryAction.RETRY_LATER);
        assertThat(decision.errorCode()).isEqualTo("file_storage.request_failed");
        assertThat(decision.errorMessage()).isEqualTo("temporary");
        assertThat(decision.nextRetryAt()).isEqualTo(OffsetDateTime.now(CLOCK).plusSeconds(10));
    }

    @Test
    @DisplayName("Retry policy использует последний backoff для попыток сверх конфигурации")
    void givenAttemptAboveConfiguredBackoff_whenDecide_thenUseLastValue() {
        docTemplateProperties.getGeneration().setMaxAttempts(4);
        docTemplateProperties.getGeneration().setRetryBackoffSeconds(java.util.List.of(10L, 30L));

        var decision = systemUnderTest.decide(
            3,
            new GenerationErrorDecision("file_storage.request_failed", "temporary", true)
        );

        assertThat(decision.action()).isEqualTo(GenerationRetryAction.RETRY_LATER);
        assertThat(decision.nextRetryAt()).isEqualTo(OffsetDateTime.now(CLOCK).plusSeconds(30));
    }

    @Test
    @DisplayName("Retry policy возвращает FAIL_FINAL для non-retriable ошибки")
    void givenNonRetriableError_whenDecide_thenReturnFailFinal() {
        docTemplateProperties.getGeneration().setMaxAttempts(4);

        var decision = systemUnderTest.decide(
            1,
            new GenerationErrorDecision("system.unexpected", "boom", false)
        );

        assertThat(decision.action()).isEqualTo(GenerationRetryAction.FAIL_FINAL);
        assertThat(decision.nextRetryAt()).isNull();
    }

    @Test
    @DisplayName("Retry policy возвращает FAIL_FINAL когда лимит попыток исчерпан")
    void givenAttemptsExhausted_whenDecide_thenReturnFailFinal() {
        docTemplateProperties.getGeneration().setMaxAttempts(3);
        docTemplateProperties.getGeneration().setRetryBackoffSeconds(java.util.List.of(10L, 30L));

        var decision = systemUnderTest.decide(
            3,
            new GenerationErrorDecision("file_storage.request_failed", "temporary", true)
        );

        assertThat(decision.action()).isEqualTo(GenerationRetryAction.FAIL_FINAL);
        assertThat(decision.nextRetryAt()).isNull();
    }
}
