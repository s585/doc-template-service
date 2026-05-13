package ru.sberbank.sbercrm.saas.doctemplate;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.zonky.test.db.AutoConfigureEmbeddedDatabase;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import ru.sberbank.sbercrm.saas.doctemplate.document.gateway.businessobject.BusinessObjectWireMock;
import ru.sberbank.sbercrm.saas.doctemplate.template.TemplateMother;
import ru.sberbank.sbercrm.saas.doctemplate.template.gateway.filestorage.FileStorageWireMock;

import java.util.UUID;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@AutoConfigureEmbeddedDatabase
@AutoConfigureWireMock(port = 0)
@ActiveProfiles("integration-test")
@Import({TemplateMother.class, BusinessObjectWireMock.class, FileStorageWireMock.class})
public abstract class AbstractIntegrationTest {
    protected static final UUID TENANT_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    protected static final UUID USER_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    protected static final UUID ENTITY_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    @Autowired
    protected TemplateMother templateMother;

    @Autowired
    protected BusinessObjectWireMock businessObjectWireMock;

    @Autowired
    protected FileStorageWireMock fileStorageWireMock;
}
