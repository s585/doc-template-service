package ru.sberbank.sbercrm.saas.doctemplate.template.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "saas.doc-template")
public class TemplateProperties {
    private final Template template = new Template();
    private final FileStorage fileStorage = new FileStorage();
    private final Generation generation = new Generation();

    @Data
    public static class Template {
        private final Variable variable = new Variable();
    }

    @Data
    public static class Variable {
        private String placeholderRegex;
    }

    @Data
    public static class FileStorage {
        private String source;
        private String folder;
        private boolean stubEnabled;
        private String stubRootPath;
    }

    @Data
    public static class Generation {
        private boolean enabled = true;
        private long dispatchFixedDelayMs = 250;
        private int workerPoolSize = 4;
    }
}
