package ru.sberbank.sbercrm.saas.doctemplate.document.service;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;
import ru.sberbank.sbercrm.saas.doctemplate.document.model.GenerationRetryDecision;

@Component
public class MicrometerGenerationJobMetricsPublisher implements GenerationJobMetricsPublisher {
    private static final String METRIC_EXHAUSTED_RETRIES = "doc_template.generation.retries.exhausted";
    private static final String METRIC_RECOVERY_FAILURES = "doc_template.generation.recovery.failures";
    private static final String TAG_ERROR_CODE = "error_code";
    private static final String UNKNOWN_ERROR_CODE = "unknown";

    private final ObjectProvider<MeterRegistry> meterRegistryProvider;

    public MicrometerGenerationJobMetricsPublisher(ObjectProvider<MeterRegistry> meterRegistryProvider) {
        this.meterRegistryProvider = meterRegistryProvider;
    }

    @Override
    public void incrementExhaustedRetries(GenerationRetryDecision retryDecision) {
        MeterRegistry meterRegistry = meterRegistryProvider.getIfAvailable();
        if (meterRegistry == null) {
            return;
        }
        meterRegistry.counter(
            METRIC_EXHAUSTED_RETRIES,
            TAG_ERROR_CODE,
            retryDecision.errorCode() == null ? UNKNOWN_ERROR_CODE : retryDecision.errorCode()
        ).increment();
    }

    @Override
    public void incrementRecoveryFailure() {
        MeterRegistry meterRegistry = meterRegistryProvider.getIfAvailable();
        if (meterRegistry == null) {
            return;
        }
        meterRegistry.counter(METRIC_RECOVERY_FAILURES).increment();
    }
}
