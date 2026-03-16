package ru.sberbank.sbercrm.doctemplate.template.processor;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFHeader;
import org.apache.poi.xwpf.usermodel.XWPFFooter;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ru.sberbank.sbercrm.doctemplate.template.config.TemplateProperties;
import ru.sberbank.sbercrm.doctemplate.template.model.MappingScope;
import ru.sberbank.sbercrm.doctemplate.template.model.TemplateVariableInfo;

class DocxTemplateProcessorTest {

    @Test
    @DisplayName("DOCX процессор извлекает переменные из текста, таблиц, header и footer")
    void givenDocxContent_whenExtractVariables_thenReturnVariablesWithExpectedScopes() throws IOException {
        // given
        TemplateProperties templateProperties = new TemplateProperties();
        templateProperties.getTemplate().getVariable().setPlaceholderRegex("\\$\\{([A-Za-z0-9_.$]+)}");
        DocxTemplateProcessor processor = new DocxTemplateProcessor(templateProperties);
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
