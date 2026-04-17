package ru.sberbank.sbercrm.saas.doctemplate.application.integration.client.config;

import feign.Retryer;
import org.springframework.context.annotation.Bean;

public class CoreClientFeignConfig {
    @Bean
    public Retryer coreClientRetryer(CoreClientRetryProperties retryProperties) {
        return new Retryer.Default(
            retryProperties.getPeriodMs(),
            retryProperties.getMaxPeriodMs(),
            retryProperties.getMaxAttempts()
        );
    }
}
