package ru.sberbank.sbercrm.saas.doctemplate.template.properties;

import java.nio.file.Files;
import java.nio.file.Path;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "saas.doc-template.file-storage")
public class FileStorageProperties {
    private String namespace;
    private final External external = new External();
    private final Local local = new Local();

    @Data
    public static class External {
        private String folder = "/doc-template";
    }

    @Data
    public static class Local {
        private boolean enabled;
        private String rootPath = resolveDefaultRootPath();
        private String templatesFolder = "templates";
        private String documentsFolder = "documents";

        private static String resolveDefaultRootPath() {
            Path currentPath = Path.of(System.getProperty("user.dir")).toAbsolutePath().normalize();
            Path projectRoot = currentPath;
            while (projectRoot != null) {
                if (Files.exists(projectRoot.resolve(".git"))) {
                    return projectRoot.toString();
                }
                projectRoot = projectRoot.getParent();
            }
            return currentPath.toString();
        }
    }
}
