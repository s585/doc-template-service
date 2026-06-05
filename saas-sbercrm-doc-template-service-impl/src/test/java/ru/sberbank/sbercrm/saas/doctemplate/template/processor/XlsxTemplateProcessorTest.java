package ru.sberbank.sbercrm.saas.doctemplate.template.processor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ru.sberbank.sbercrm.saas.doctemplate.application.exception.model.BusinessCrmException;
import ru.sberbank.sbercrm.saas.doctemplate.document.model.CollectionDataset;
import ru.sberbank.sbercrm.saas.doctemplate.document.model.GenerationTemplateContext;
import ru.sberbank.sbercrm.saas.doctemplate.template.constant.TemplateConstants;
import ru.sberbank.sbercrm.saas.doctemplate.template.model.MappingScope;
import ru.sberbank.sbercrm.saas.doctemplate.template.model.TemplateVariableInfo;
import ru.sberbank.sbercrm.saas.doctemplate.template.properties.DocTemplateProperties;

class XlsxTemplateProcessorTest {

    @Test
    @DisplayName("Процессор XLSX извлекает переменные из ячеек и по умолчанию помечает их областью VALUE")
    void givenXlsxContent_whenExtractVariables_thenReturnVariablesWithValueScope() throws IOException {
        XlsxTemplateProcessor systemUnderTest = createProcessor();
        byte[] content = createXlsxCollectionContent();

        List<TemplateVariableInfo> variables = systemUnderTest.extractVariables(content);

        assertThat(variables)
            .extracting(TemplateVariableInfo::getKey, TemplateVariableInfo::getScope)
            .containsExactlyInAnyOrder(
                org.assertj.core.groups.Tuple.tuple("deal_number", MappingScope.VALUE),
                org.assertj.core.groups.Tuple.tuple("product_name", MappingScope.VALUE),
                org.assertj.core.groups.Tuple.tuple("product_qty", MappingScope.VALUE)
            );
    }

    @Test
    @DisplayName("Процессор XLSX размножает строку по строкам коллекции")
    void givenXlsxCollectionRow_whenGenerate_thenRepeatTemplateRow() throws IOException {
        XlsxTemplateProcessor systemUnderTest = createProcessor();
        byte[] content = createXlsxCollectionContent();

        byte[] generated = systemUnderTest.generate(
            content,
            GenerationTemplateContext.builder()
                .scalarValues(Map.of("deal_number", "D-123"))
                .collections(List.of(
                    CollectionDataset.builder()
                        .keys(new LinkedHashSet<>(List.of("product_name", "product_qty")))
                        .rows(List.of(
                            Map.of("product_name", "Product A", "product_qty", "2"),
                            Map.of("product_name", "Product B", "product_qty", "1")
                        ))
                        .build()
                ))
                .build()
        );

        try (var workbook = WorkbookFactory.create(new ByteArrayInputStream(generated))) {
            var sheet = workbook.getSheetAt(0);
            assertThat(sheet.getRow(0).getCell(0).getStringCellValue()).isEqualTo("D-123");
            assertThat(sheet.getRow(1).getCell(0).getStringCellValue()).isEqualTo("Product A");
            assertThat(sheet.getRow(1).getCell(1).getStringCellValue()).isEqualTo("2");
            assertThat(sheet.getRow(2).getCell(0).getStringCellValue()).isEqualTo("Product B");
            assertThat(sheet.getRow(2).getCell(1).getStringCellValue()).isEqualTo("1");
        }
    }

    @Test
    @DisplayName("Процессор XLSX удаляет шаблонную строку для пустой коллекции")
    void givenEmptyCollection_whenGenerate_thenRemoveTemplateRow() throws IOException {
        XlsxTemplateProcessor systemUnderTest = createProcessor();
        byte[] content = createXlsxCollectionContent();

        byte[] generated = systemUnderTest.generate(
            content,
            GenerationTemplateContext.builder()
                .scalarValues(Map.of("deal_number", "D-123"))
                .collections(List.of(
                    CollectionDataset.builder()
                        .keys(new LinkedHashSet<>(List.of("product_name", "product_qty")))
                        .rows(List.of())
                        .build()
                ))
                .build()
        );

        try (var workbook = WorkbookFactory.create(new ByteArrayInputStream(generated))) {
            var sheet = workbook.getSheetAt(0);
            assertThat(sheet.getRow(0).getCell(0).getStringCellValue()).isEqualTo("D-123");
            assertThat(sheet.getRow(1)).isNull();
        }
    }

    @Test
    @DisplayName("Процессор XLSX падает, если collection placeholder не покрыт dataset")
    void givenCollectionPlaceholderWithoutDataset_whenGenerate_thenThrowDetailedError() throws IOException {
        XlsxTemplateProcessor systemUnderTest = createProcessor();
        byte[] content = createXlsxMissingDatasetContent();

        assertThatThrownBy(() -> systemUnderTest.generate(
            content,
            GenerationTemplateContext.builder()
                .scalarValues(Map.of("deal_number", "D-123"))
                .collections(List.of(
                    CollectionDataset.builder()
                        .keys(new LinkedHashSet<>(List.of("product_name")))
                        .rows(List.of(Map.of("product_name", "Product A")))
                        .build()
                ))
                .build()
        ))
            .isInstanceOf(BusinessCrmException.class)
            .satisfies(throwable -> {
                BusinessCrmException exception = (BusinessCrmException) throwable;
                assertThat(exception.getCode()).isEqualTo(TemplateConstants.ErrorCodes.TEMPLATE_COLLECTION_PLACEHOLDERS_MISSING_DATASET);
                assertThat(exception.getParams()).containsExactly("[product_qty]");
            });
    }

    @Test
    @DisplayName("Процессор XLSX падает, если collection placeholders неоднозначны между dataset'ами")
    void givenAmbiguousDatasets_whenGenerate_thenThrowDetailedError() throws IOException {
        XlsxTemplateProcessor systemUnderTest = createProcessor();
        byte[] content = createXlsxSingleCollectionRowContent();

        assertThatThrownBy(() -> systemUnderTest.generate(
            content,
            GenerationTemplateContext.builder()
                .collections(List.of(
                    CollectionDataset.builder()
                        .keys(new LinkedHashSet<>(List.of("product_name")))
                        .rows(List.of(Map.of("product_name", "Product A")))
                        .build(),
                    CollectionDataset.builder()
                        .keys(new LinkedHashSet<>(List.of("product_name", "product_qty")))
                        .rows(List.of(Map.of("product_name", "Product B", "product_qty", "2")))
                        .build()
                ))
                .build()
        ))
            .isInstanceOf(BusinessCrmException.class)
            .satisfies(throwable -> {
                BusinessCrmException exception = (BusinessCrmException) throwable;
                assertThat(exception.getCode()).isEqualTo(TemplateConstants.ErrorCodes.TEMPLATE_COLLECTION_PLACEHOLDERS_AMBIGUOUS);
                assertThat(exception.getParams()).containsExactly("[product_name]");
            });
    }

    @Test
    @DisplayName("Процессор XLSX подставляет scalar value в обычную строку без коллекции")
    void givenScalarRow_whenGenerate_thenReplaceOnlyScalarPlaceholders() throws IOException {
        XlsxTemplateProcessor systemUnderTest = createProcessor();
        byte[] content = createXlsxScalarContent();

        byte[] generated = systemUnderTest.generate(
            content,
            GenerationTemplateContext.builder()
                .scalarValues(Map.of("deal_number", "D-123"))
                .collections(List.of())
                .build()
        );

        try (var workbook = WorkbookFactory.create(new ByteArrayInputStream(generated))) {
            var sheet = workbook.getSheetAt(0);
            assertThat(sheet.getRow(0).getCell(0).getStringCellValue()).isEqualTo("Договор D-123");
            assertThat(sheet.getRow(0).getCell(1).getNumericCellValue()).isEqualTo(42d);
        }
    }

    @Test
    @DisplayName("Процессор XLSX сохраняет типы ячеек при размножении collection-строки")
    void givenCollectionRowWithTypedCells_whenGenerate_thenPreserveCellTypesAndValues() throws IOException {
        XlsxTemplateProcessor systemUnderTest = createProcessor();
        byte[] content = createTypedCollectionContent();

        byte[] generated = systemUnderTest.generate(
            content,
            GenerationTemplateContext.builder()
                .scalarValues(Map.of("deal_number", "D-123"))
                .collections(List.of(
                    CollectionDataset.builder()
                        .keys(new LinkedHashSet<>(List.of("product_name")))
                        .rows(List.of(
                            Map.of("product_name", "Product A"),
                            Map.of("product_name", "Product B")
                        ))
                        .build()
                ))
                .build()
        );

        try (var workbook = WorkbookFactory.create(new ByteArrayInputStream(generated))) {
            var sheet = workbook.getSheetAt(0);

            assertThat(sheet.getRow(1).getCell(0).getStringCellValue()).isEqualTo("Product A");
            assertThat(sheet.getRow(1).getCell(1).getNumericCellValue()).isEqualTo(10d);
            assertThat(sheet.getRow(1).getCell(2).getBooleanCellValue()).isTrue();
            assertThat(sheet.getRow(1).getCell(3).getCellType()).isEqualTo(CellType.FORMULA);
            assertThat(sheet.getRow(1).getCell(3).getCellFormula()).isEqualTo("B2*2");
            assertThat(sheet.getRow(1).getCell(4).getCellType()).isEqualTo(CellType.BLANK);

            assertThat(sheet.getRow(2).getCell(0).getStringCellValue()).isEqualTo("Product B");
            assertThat(sheet.getRow(2).getCell(1).getNumericCellValue()).isEqualTo(10d);
            assertThat(sheet.getRow(2).getCell(2).getBooleanCellValue()).isTrue();
            assertThat(sheet.getRow(2).getCell(3).getCellType()).isEqualTo(CellType.FORMULA);
            assertThat(sheet.getRow(2).getCell(3).getCellFormula()).isEqualTo("B2*2");
            assertThat(sheet.getRow(2).getCell(4).getCellType()).isEqualTo(CellType.BLANK);
        }
    }

    @Test
    @DisplayName("Процессор XLSX сохраняет стиль строки и ячейки при размножении")
    void givenStyledCollectionRow_whenGenerate_thenPreserveStyles() throws IOException {
        XlsxTemplateProcessor systemUnderTest = createProcessor();
        byte[] content = createStyledCollectionContent();

        byte[] generated = systemUnderTest.generate(
            content,
            GenerationTemplateContext.builder()
                .collections(List.of(
                    CollectionDataset.builder()
                        .keys(new LinkedHashSet<>(List.of("product_name")))
                        .rows(List.of(Map.of("product_name", "Product A")))
                        .build()
                ))
                .build()
        );

        try (var workbook = WorkbookFactory.create(new ByteArrayInputStream(generated))) {
            var sheet = workbook.getSheetAt(0);
            var row = sheet.getRow(0);
            assertThat(row.getHeight()).isEqualTo((short) 420);
            assertThat(row.getCell(0).getCellStyle().getDataFormat()).isEqualTo((short) 7);
        }
    }

    private XlsxTemplateProcessor createProcessor() {
        DocTemplateProperties docTemplateProperties = new DocTemplateProperties();
        docTemplateProperties.getTemplate().getVariable().setPlaceholderRegex("\\$\\{([A-Za-z0-9_.$]+)}");
        return new XlsxTemplateProcessor(docTemplateProperties);
    }

    private byte[] createXlsxCollectionContent() throws IOException {
        try (XSSFWorkbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            var sheet = workbook.createSheet("template");
            var headerRow = sheet.createRow(0);
            headerRow.createCell(0).setCellValue("${deal_number}");
            var templateRow = sheet.createRow(1);
            templateRow.createCell(0).setCellValue("${product_name}");
            templateRow.createCell(1).setCellValue("${product_qty}");
            workbook.write(outputStream);
            return outputStream.toByteArray();
        }
    }

    private byte[] createXlsxMissingDatasetContent() throws IOException {
        try (XSSFWorkbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            var sheet = workbook.createSheet("template");
            var row = sheet.createRow(0);
            row.createCell(0).setCellValue("${product_name}");
            row.createCell(1).setCellValue("${product_qty}");
            workbook.write(outputStream);
            return outputStream.toByteArray();
        }
    }

    private byte[] createXlsxSingleCollectionRowContent() throws IOException {
        try (XSSFWorkbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            var sheet = workbook.createSheet("template");
            var row = sheet.createRow(0);
            row.createCell(0).setCellValue("${product_name}");
            workbook.write(outputStream);
            return outputStream.toByteArray();
        }
    }

    private byte[] createXlsxScalarContent() throws IOException {
        try (XSSFWorkbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            var sheet = workbook.createSheet("template");
            var row = sheet.createRow(0);
            row.createCell(0).setCellValue("Договор ${deal_number}");
            row.createCell(1).setCellValue(42d);
            workbook.write(outputStream);
            return outputStream.toByteArray();
        }
    }

    private byte[] createTypedCollectionContent() throws IOException {
        try (XSSFWorkbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            var sheet = workbook.createSheet("template");
            var headerRow = sheet.createRow(0);
            headerRow.createCell(0).setCellValue("${deal_number}");

            var templateRow = sheet.createRow(1);
            templateRow.createCell(0).setCellValue("${product_name}");
            templateRow.createCell(1).setCellValue(10d);
            templateRow.createCell(2).setCellValue(true);
            templateRow.createCell(3).setCellFormula("B2*2");
            templateRow.createCell(4).setBlank();

            workbook.write(outputStream);
            return outputStream.toByteArray();
        }
    }

    private byte[] createStyledCollectionContent() throws IOException {
        try (XSSFWorkbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            var sheet = workbook.createSheet("template");
            var row = sheet.createRow(0);
            row.setHeight((short) 420);
            var style = workbook.createCellStyle();
            style.setDataFormat((short) 7);
            var cell = row.createCell(0);
            cell.setCellValue("${product_name}");
            cell.setCellStyle(style);
            workbook.write(outputStream);
            return outputStream.toByteArray();
        }
    }
}
