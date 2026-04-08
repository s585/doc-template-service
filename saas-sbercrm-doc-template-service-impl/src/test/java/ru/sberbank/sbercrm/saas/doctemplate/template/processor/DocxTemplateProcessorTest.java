package ru.sberbank.sbercrm.saas.doctemplate.template.processor;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFHeader;
import org.apache.poi.xwpf.usermodel.XWPFFooter;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ru.sberbank.sbercrm.saas.doctemplate.template.properties.DocTemplateProperties;
import ru.sberbank.sbercrm.saas.doctemplate.template.model.MappingScope;
import ru.sberbank.sbercrm.saas.doctemplate.template.model.TemplateVariableInfo;

class DocxTemplateProcessorTest {

    @Test
    @DisplayName("DOCX процессор извлекает переменные из текста, таблиц, header и footer")
    void givenDocxContent_whenExtractVariables_thenReturnVariablesWithExpectedScopes() throws IOException {
        // given
        DocTemplateProperties docTemplateProperties = new DocTemplateProperties();
        docTemplateProperties.getTemplate().getVariable().setPlaceholderRegex("\\$\\{([A-Za-z0-9_.$]+)}");
        DocxTemplateProcessor processor = new DocxTemplateProcessor(docTemplateProperties);
        byte[] content = createDocxContent();

        // when
        List<TemplateVariableInfo> variables = processor.extractVariables(content);

        // then
        assertThat(variables)
            .extracting(TemplateVariableInfo::getKey, TemplateVariableInfo::getScope)
            .containsExactlyInAnyOrder(
                org.assertj.core.groups.Tuple.tuple("deal_number", MappingScope.VALUE),
                org.assertj.core.groups.Tuple.tuple("header_number", MappingScope.VALUE),
                org.assertj.core.groups.Tuple.tuple("footer_number", MappingScope.VALUE),
                org.assertj.core.groups.Tuple.tuple("product_name", MappingScope.TABLE)
            );
    }

    @Test
    @DisplayName("DOCX процессор подставляет значения переменных при генерации")
    void givenDocxContentAndValues_whenGenerate_thenReplacePlaceholders() throws IOException {
        // given
        DocTemplateProperties docTemplateProperties = new DocTemplateProperties();
        docTemplateProperties.getTemplate().getVariable().setPlaceholderRegex("\\$\\{([A-Za-z0-9_.$]+)}");
        DocxTemplateProcessor processor = new DocxTemplateProcessor(docTemplateProperties);
        byte[] content = createDocxContent();

        // when
        byte[] generated = processor.generate(content, Map.of(
            "deal_number", "42",
            "header_number", "H-1",
            "footer_number", "F-1",
            "product_name", "Product"
        ));

        // then
        try (XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(generated))) {
            assertThat(document.getParagraphs().getFirst().getText()).contains("42");
            assertThat(document.getHeaderList().getFirst().getText()).contains("H-1");
            assertThat(document.getFooterList().getFirst().getText()).contains("F-1");
            assertThat(document.getTables().getFirst().getRow(0).getCell(0).getText()).contains("Product");
        }
    }

    private byte[] createDocxContent() throws IOException {
        try (XWPFDocument document = new XWPFDocument();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            document.createParagraph().createRun().setText("Номер сделки ${deal_number}");

            XWPFHeader header = document.createHeader(org.apache.poi.wp.usermodel.HeaderFooterType.DEFAULT);
            header.createParagraph().createRun().setText("Header ${header_number}");

            XWPFFooter footer = document.createFooter(org.apache.poi.wp.usermodel.HeaderFooterType.DEFAULT);
            footer.createParagraph().createRun().setText("Footer ${footer_number}");

            XWPFTable table = document.createTable(1, 1);
            table.getRow(0).getCell(0).setText("${product_name}");

            document.write(outputStream);
            return outputStream.toByteArray();
        }
    }
}
