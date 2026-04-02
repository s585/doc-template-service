package ru.sberbank.sbercrm.saas.doctemplate.document.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import ru.sberbank.sbercrm.saas.doctemplate.template.properties.TemplateProperties;

@Configuration
public class GenerationJobExecutorConfig {
    @Bean
    public ThreadPoolTaskExecutor generationJobTaskExecutor(TemplateProperties templateProperties) {
        int poolSize = templateProperties.getGeneration().getWorkerPoolSize();
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setThreadNamePrefix("generation-job-");
        executor.setCorePoolSize(poolSize);
        executor.setMaxPoolSize(poolSize);
        executor.setQueueCapacity(0);
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.initialize();
        return executor;
    }
}
