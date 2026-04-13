package ru.sberbank.sbercrm.saas.doctemplate.document.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class GenerationWorkerIdentityProviderImplTest {
    private static final UUID WORKER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    @Test
    @DisplayName("Provider возвращает стабильный worker identity")
    void givenIdentity_whenRequested_thenReturnConfiguredValues() {
        GenerationWorkerIdentityProviderImpl systemUnderTest =
            new GenerationWorkerIdentityProviderImpl(WORKER_ID, "doc-template@host:123");

        assertThat(systemUnderTest.getWorkerId()).isEqualTo(WORKER_ID);
        assertThat(systemUnderTest.getWorkerName()).isEqualTo("doc-template@host:123");
        assertThat(systemUnderTest.getExecutionName()).startsWith("doc-template@host:123:");
    }
}
