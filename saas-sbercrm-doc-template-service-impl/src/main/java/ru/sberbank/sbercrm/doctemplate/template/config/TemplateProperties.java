package ru.sberbank.sbercrm.doctemplate.template.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "saas.doc-template")
public class TemplateProperties {
    private final Template template = new Template();
    private final FileStorage fileStorage = new FileStorage();

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
    }
}
