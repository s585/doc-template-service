package ru.sberbank.sbercrm.saas.doctemplate.template.processor;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ru.sberbank.sbercrm.saas.doctemplate.template.properties.TemplateProperties;
import ru.sberbank.sbercrm.saas.doctemplate.template.model.MappingScope;
import ru.sberbank.sbercrm.saas.doctemplate.template.model.TemplateVariableInfo;
import ru.sberbank.sbercrm.saas.doctemplate.template.processor.XlsxTemplateProcessor;

class XlsxTemplateProcessorTest {

    @Test
    @DisplayName("XLSX процессор извлекает переменные из ячеек и помечает их scope TABLE")
    void givenXlsxContent_whenExtractVariables_thenReturnVariablesWithTableScope() throws IOException {
        // given
        TemplateProperties templateProperties = new TemplateProperties();
        templateProperties.getTemplate().getVariable().setPlaceholderRegex("\\$\\{([A-Za-z0-9_.$]+)}");
        XlsxTemplateProcessor processor = new XlsxTemplateProcessor(templateProperties);
        byte[] content = createXlsxContent();

        // when
        List<TemplateVariableInfo> variables = processor.extractVariables(content);

        // then
        assertThat(variables)
            .extracting(TemplateVariableInfo::getKey, TemplateVariableInfo::getScope)
            .containsExactlyInAnyOrder(
                org.assertj.core.groups.Tuple.tuple("deal_number", MappingScope.TABLE),
                org.assertj.core.groups.Tuple.tuple("product_name", MappingScope.TABLE)
            );
    }

    @Test
    @DisplayName("XLSX процессор подставляет значения переменных при генерации")
    void givenXlsxContentAndValues_whenGenerate_thenReplacePlaceholders() throws IOException {
        // given
        TemplateProperties templateProperties = new TemplateProperties();
        templateProperties.getTemplate().getVariable().setPlaceholderRegex("\\$\\{([A-Za-z0-9_.$]+)}");
        XlsxTemplateProcessor processor = new XlsxTemplateProcessor(templateProperties);
        byte[] content = createXlsxContent();

        // when
        byte[] generated = processor.generate(content, Map.of(
            "deal_number", "42",
            "product_name", "Product"
        ));

        // then
        try (var workbook = WorkbookFactory.create(new ByteArrayInputStream(generated))) {
            var row = workbook.getSheetAt(0).getRow(0);
            assertThat(row.getCell(0).getStringCellValue()).isEqualTo("42");
            assertThat(row.getCell(1).getStringCellValue()).isEqualTo("Product");
        }
    }

    private byte[] createXlsxContent() throws IOException {
        try (XSSFWorkbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            var sheet = workbook.createSheet("template");
            var row = sheet.createRow(0);
            row.createCell(0).setCellValue("${deal_number}");
            row.createCell(1).setCellValue("${product_name}");
            workbook.write(outputStream);
            return outputStream.toByteArray();
        }
    }
}
