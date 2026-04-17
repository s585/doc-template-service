package ru.sberbank.sbercrm.saas.doctemplate.application.integration.client.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "saas.doc-template.integration.core-client.retry")
public class CoreClientRetryProperties {
    private long periodMs = 100L;
    private long maxPeriodMs = 500L;
    private int maxAttempts = 3;
}
