package ru.sberbank.sbercrm.saas.doctemplate.application.integration.gateway;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ru.sberbank.sbercrm.saas.doctemplate.document.model.GenerationJob;
import ru.sberbank.sbercrm.saas.doctemplate.template.properties.FileStorageProperties;

class FileStoragePathResolverTest {
    private static final UUID ENTITY_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID OBJECT_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID DOCUMENT_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");

    @Test
    @DisplayName("External resolver строит пути в общем folder file-storage")
    void givenExternalStorage_whenResolvePaths_thenUseFileStorageFolder() {
        FileStorageProperties properties = new FileStorageProperties();
        properties.getExternal().setFolder("/doc-template");
        FileStoragePathResolver resolver = new ExternalFileStoragePathResolver(properties);

        assertThat(resolver.templateFolder(ENTITY_ID))
            .isEqualTo("/doc-template/" + ENTITY_ID);
        assertThat(resolver.generatedFolder(buildJob()))
            .isEqualTo("/doc-template/generated/" + ENTITY_ID + "/" + OBJECT_ID + "/" + DOCUMENT_ID);
    }

    @Test
    @DisplayName("Local resolver строит пути в локальные папки templates и documents")
    void givenLocalStorage_whenResolvePaths_thenUseLocalFolders() {
        FileStorageProperties properties = new FileStorageProperties();
        properties.getLocal().setTemplatesFolder("templates");
        properties.getLocal().setDocumentsFolder("documents");
        FileStoragePathResolver resolver = new LocalFileStoragePathResolver(properties);

        assertThat(resolver.templateFolder(ENTITY_ID))
            .isEqualTo("templates/" + ENTITY_ID);
        assertThat(resolver.generatedFolder(buildJob()))
            .isEqualTo("documents/" + ENTITY_ID + "/" + OBJECT_ID + "/" + DOCUMENT_ID);
    }

    private GenerationJob buildJob() {
        return GenerationJob.builder()
            .entityId(ENTITY_ID)
            .objectId(OBJECT_ID)
            .documentId(DOCUMENT_ID)
            .build();
    }
}
