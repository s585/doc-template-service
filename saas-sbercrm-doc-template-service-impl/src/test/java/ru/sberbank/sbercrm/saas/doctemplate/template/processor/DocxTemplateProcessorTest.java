package ru.sberbank.sbercrm.saas.doctemplate.template.processor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import org.apache.poi.wp.usermodel.HeaderFooterType;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFHeader;
import org.apache.poi.xwpf.usermodel.XWPFFooter;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ru.sberbank.sbercrm.saas.doctemplate.application.exception.model.BusinessCrmException;
import ru.sberbank.sbercrm.saas.doctemplate.document.model.CollectionDataset;
import ru.sberbank.sbercrm.saas.doctemplate.document.model.GenerationTemplateContext;
import ru.sberbank.sbercrm.saas.doctemplate.template.constant.TemplateConstants;
import ru.sberbank.sbercrm.saas.doctemplate.template.properties.DocTemplateProperties;
import ru.sberbank.sbercrm.saas.doctemplate.template.model.MappingScope;
import ru.sberbank.sbercrm.saas.doctemplate.template.model.TemplateFormat;
import ru.sberbank.sbercrm.saas.doctemplate.template.model.TemplateVariableInfo;

class DocxTemplateProcessorTest {

    @Test
    @DisplayName("Процессор поддерживает только DOCX формат")
    void whenSupports_thenReturnTrueOnlyForDocx() {
        DocxTemplateProcessor systemUnderTest = createProcessor();

        assertThat(systemUnderTest.supports(TemplateFormat.DOCX)).isTrue();
        assertThat(systemUnderTest.supports(TemplateFormat.XLSX)).isFalse();
    }

    @Test
    @DisplayName("Процессор DOCX падает бизнес-ошибкой при извлечении переменных из битого файла")
    void givenInvalidContent_whenExtractVariables_thenThrowBusinessException() {
        DocxTemplateProcessor systemUnderTest = createProcessor();

        assertThatThrownBy(() -> systemUnderTest.extractVariables("not-a-docx".getBytes()))
            .isInstanceOf(BusinessCrmException.class)
            .satisfies(throwable -> {
                BusinessCrmException exception = (BusinessCrmException) throwable;
                assertThat(exception.getCode()).isEqualTo(TemplateConstants.ErrorCodes.TEMPLATE_PARSING_FAILED);
                assertThat(exception.getParams()).containsExactly(TemplateFormat.DOCX.value());
            });
    }

    @Test
    @DisplayName("Процессор DOCX падает бизнес-ошибкой при генерации из битого файла")
    void givenInvalidContent_whenGenerate_thenThrowBusinessException() {
        DocxTemplateProcessor systemUnderTest = createProcessor();

        assertThatThrownBy(() -> systemUnderTest.generate(
            "not-a-docx".getBytes(),
            GenerationTemplateContext.builder()
                .scalarValues(Map.of())
                .collections(List.of())
                .build()
        ))
            .isInstanceOf(BusinessCrmException.class)
            .satisfies(throwable -> {
                BusinessCrmException exception = (BusinessCrmException) throwable;
                assertThat(exception.getCode()).isEqualTo(TemplateConstants.ErrorCodes.TEMPLATE_PARSING_FAILED);
                assertThat(exception.getParams()).containsExactly(TemplateFormat.DOCX.value());
            });
    }

    @Test
    @DisplayName("Процессор DOCX извлекает переменные из текста, таблиц, верхнего и нижнего колонтитулов")
    void givenDocxContent_whenExtractVariables_thenReturnVariablesWithExpectedScopes() throws IOException {
        // given
        DocTemplateProperties docTemplateProperties = new DocTemplateProperties();
        docTemplateProperties.getTemplate().getVariable().setPlaceholderRegex("\\$\\{([A-Za-z0-9_.$]+)}");
        DocxTemplateProcessor systemUnderTest = new DocxTemplateProcessor(docTemplateProperties);
        byte[] content = createDocxContent();

        // when
        List<TemplateVariableInfo> variables = systemUnderTest.extractVariables(content);

        // then
        assertThat(variables)
            .extracting(TemplateVariableInfo::getKey, TemplateVariableInfo::getScope, TemplateVariableInfo::getBlockId)
            .containsExactlyInAnyOrder(
                org.assertj.core.groups.Tuple.tuple("deal_number", MappingScope.VALUE, null),
                org.assertj.core.groups.Tuple.tuple("header_number", MappingScope.VALUE, null),
                org.assertj.core.groups.Tuple.tuple("footer_number", MappingScope.VALUE, null),
                org.assertj.core.groups.Tuple.tuple(
                    "product_name",
                    MappingScope.COLLECTION,
                    "docx:block:001:product_name"
                ),
                org.assertj.core.groups.Tuple.tuple(
                    "payment_amount",
                    MappingScope.COLLECTION,
                    "docx:block:002:payment_amount"
                ),
                org.assertj.core.groups.Tuple.tuple("currency", MappingScope.COLLECTION, "docx:block:002:payment_amount")
            );
    }

    @Test
    @DisplayName("Процессор DOCX подставляет значения переменных при генерации")
    void givenDocxContentAndValues_whenGenerate_thenReplacePlaceholders() throws IOException {
        // given
        DocxTemplateProcessor systemUnderTest = createProcessor();
        byte[] content = createDocxContent();

        // when
        byte[] generated = systemUnderTest.generate(
            content,
            GenerationTemplateContext.builder()
                .scalarValues(Map.of(
                    "deal_number", "42",
                    "header_number", "H-1",
                    "footer_number", "F-1",
                    "currency", "RUB"
                ))
                .collections(List.of(
                    CollectionDataset.builder()
                        .keys(new LinkedHashSet<>(List.of("product_name")))
                        .rows(List.of(Map.of("product_name", "Product")))
                        .build(),
                    CollectionDataset.builder()
                        .keys(new LinkedHashSet<>(List.of("payment_amount")))
                        .rows(List.of(
                            Map.of("payment_amount", "100"),
                            Map.of("payment_amount", "250")
                        ))
                        .build()
                ))
                .build()
        );

        // then
        try (XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(generated))) {
            assertThat(document.getParagraphs().getFirst().getText()).contains("42");
            assertThat(document.getHeaderList().getFirst().getText()).contains("H-1");
            assertThat(document.getFooterList().getFirst().getText()).contains("F-1");
            assertThat(document.getTables().getFirst().getRow(0).getCell(0).getText()).contains("Header");
            assertThat(document.getTables().getFirst().getRow(1).getCell(0).getText()).contains("Product");
            assertThat(document.getParagraphs())
                .extracting(XWPFParagraph::getText)
                .contains("Платеж 100 RUB", "Платеж 250 RUB")
                .doesNotContain("Платеж ${payment_amount} ${currency}");
        }
    }

    @Test
    @DisplayName("Процессор DOCX заменяет все вхождения повторяющегося placeholder-а")
    void givenRepeatedPlaceholder_whenGenerate_thenReplaceAllOccurrences() throws IOException {
        DocxTemplateProcessor systemUnderTest = createProcessor();
        byte[] content = createDocxWithRepeatedPlaceholder();

        byte[] generated = systemUnderTest.generate(
            content,
            GenerationTemplateContext.builder()
                .scalarValues(Map.of("client_name", "Direct LLC"))
                .collections(List.of())
                .build()
        );

        try (XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(generated))) {
            assertThat(document.getParagraphs().getFirst().getText())
                .isEqualTo("Клиент Direct LLC, повтор Direct LLC");
        }
    }

    @Test
    @DisplayName("Процессор DOCX заменяет numbered paragraph как scalar, если dataset не найден")
    void givenNumberedParagraphWithoutDataset_whenGenerate_thenReplaceAsScalarParagraph() throws IOException {
        DocxTemplateProcessor systemUnderTest = createProcessor();
        byte[] content = createDocxListOnlyContent();

        byte[] generated = systemUnderTest.generate(
            content,
            GenerationTemplateContext.builder()
                .scalarValues(Map.of("payment_amount", "100"))
                .collections(List.of())
                .build()
        );

        try (XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(generated))) {
            assertThat(document.getParagraphs())
                .extracting(XWPFParagraph::getText)
                .containsExactly("Платеж 100");
        }
    }

    @Test
    @DisplayName("Процессор DOCX вставляет collection paragraph перед следующим body element")
    void givenCollectionParagraphBeforeAnotherParagraph_whenGenerate_thenInsertRowsInPlace() throws IOException {
        DocxTemplateProcessor systemUnderTest = createProcessor();
        byte[] content = createDocxListBeforeParagraphContent();

        byte[] generated = systemUnderTest.generate(
            content,
            GenerationTemplateContext.builder()
                .scalarValues(Map.of())
                .collections(List.of(
                    CollectionDataset.builder()
                        .keys(new LinkedHashSet<>(List.of("payment_amount")))
                        .rows(List.of(
                            Map.of("payment_amount", "100"),
                            Map.of("payment_amount", "250")
                        ))
                        .build()
                ))
                .build()
        );

        try (XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(generated))) {
            assertThat(document.getParagraphs())
                .extracting(XWPFParagraph::getText)
                .containsExactly("Платеж 100", "Платеж 250", "Итого");
        }
    }

    @Test
    @DisplayName("Процессор DOCX удаляет лишние runs после замены текста параграфа")
    void givenPlaceholderSplitAcrossRuns_whenGenerate_thenKeepSingleStyledRun() throws IOException {
        DocxTemplateProcessor systemUnderTest = createProcessor();
        byte[] content = createDocxParagraphWithMultipleRuns();

        byte[] generated = systemUnderTest.generate(
            content,
            GenerationTemplateContext.builder()
                .scalarValues(Map.of("client_name", "Direct LLC"))
                .collections(List.of())
                .build()
        );

        try (XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(generated))) {
            XWPFParagraph paragraph = document.getParagraphs().getFirst();
            assertThat(paragraph.getText()).isEqualTo("Клиент Direct LLC");
            assertThat(paragraph.getRuns()).hasSize(1);
            assertThat(paragraph.getRuns().getFirst().getFontSize()).isEqualTo(14);
        }
    }

    @Test
    @DisplayName("Процессор DOCX сохраняет размер шрифта при замене scalar placeholder-а")
    void givenStyledPlaceholder_whenGenerate_thenPreserveFontSize() throws IOException {
        DocxTemplateProcessor systemUnderTest = createProcessor();
        byte[] content = createStyledDocxParagraph();

        byte[] generated = systemUnderTest.generate(
            content,
            GenerationTemplateContext.builder()
                .scalarValues(Map.of("client_name", "Direct LLC"))
                .collections(List.of())
                .build()
        );

        try (XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(generated))) {
            XWPFRun run = document.getParagraphs().getFirst().getRuns().getFirst();
            assertThat(document.getParagraphs().getFirst().getText()).isEqualTo("Клиент Direct LLC");
            assertThat(run.getFontSize()).isEqualTo(18);
        }
    }

    @Test
    @DisplayName("Процессор DOCX сохраняет размер шрифта при замене placeholder-а в таблице")
    void givenStyledTablePlaceholder_whenGenerate_thenPreserveFontSize() throws IOException {
        DocxTemplateProcessor systemUnderTest = createProcessor();
        byte[] content = createStyledDocxTable();

        byte[] generated = systemUnderTest.generate(
            content,
            GenerationTemplateContext.builder()
                .scalarValues(Map.of("client_name", "Direct LLC"))
                .collections(List.of())
                .build()
        );

        try (XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(generated))) {
            XWPFParagraph paragraph = document.getTables().getFirst().getRow(0).getCell(0).getParagraphs().getFirst();
            assertThat(paragraph.getText()).isEqualTo("Клиент Direct LLC");
            assertThat(paragraph.getRuns().getFirst().getFontSize()).isEqualTo(16);
        }
    }

    @Test
    @DisplayName("Процессор DOCX размножает строку таблицы по строкам коллекции")
    void givenTableCollection_whenGenerate_thenRepeatTemplateRow() throws IOException {
        DocxTemplateProcessor systemUnderTest = createProcessor();
        byte[] content = createDocxTableContent();

        byte[] generated = systemUnderTest.generate(
            content,
            GenerationTemplateContext.builder()
                .scalarValues(Map.of("currency", "RUB"))
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

        try (XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(generated))) {
            XWPFTable table = document.getTables().getFirst();
            assertThat(table.getRows()).hasSize(3);
            assertThat(table.getRow(1).getCell(0).getText()).isEqualTo("Product A");
            assertThat(table.getRow(1).getCell(1).getText()).isEqualTo("2");
            assertThat(table.getRow(1).getCell(2).getText()).isEqualTo("RUB");
            assertThat(table.getRow(2).getCell(0).getText()).isEqualTo("Product B");
            assertThat(table.getRow(2).getCell(1).getText()).isEqualTo("1");
            assertThat(table.getRow(2).getCell(2).getText()).isEqualTo("RUB");
        }
    }

    @Test
    @DisplayName("Процессор DOCX копирует свойства строк и ячеек при размножении таблицы")
    void givenTableCollectionWithRowAndCellProperties_whenGenerate_thenCopyProperties() throws IOException {
        DocxTemplateProcessor systemUnderTest = createProcessor();
        byte[] content = createDocxTableWithRowAndCellProperties();

        byte[] generated = systemUnderTest.generate(
            content,
            GenerationTemplateContext.builder()
                .scalarValues(Map.of())
                .collections(List.of(
                    CollectionDataset.builder()
                        .keys(new LinkedHashSet<>(List.of("product_name")))
                        .rows(List.of(Map.of("product_name", "Product A")))
                        .build()
                ))
                .build()
        );

        try (XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(generated))) {
            XWPFTable table = document.getTables().getFirst();
            assertThat(table.getRow(1).getCtRow().getTrPr()).isNotNull();
            assertThat(table.getRow(1).getCell(0).getCTTc().getTcPr()).isNotNull();
            assertThat(table.getRow(1).getCell(0).getText()).isEqualTo("Product A");
        }
    }

    @Test
    @DisplayName("Процессор DOCX удаляет шаблонный collection block для пустой коллекции")
    void givenEmptyCollection_whenGenerate_thenRemoveTemplateBlock() throws IOException {
        DocxTemplateProcessor systemUnderTest = createProcessor();
        byte[] content = createDocxContent();

        byte[] generated = systemUnderTest.generate(
            content,
            GenerationTemplateContext.builder()
                .scalarValues(Map.of(
                    "deal_number", "42",
                    "header_number", "H-1",
                    "footer_number", "F-1",
                    "currency", "RUB"
                ))
                .collections(List.of(
                    CollectionDataset.builder()
                        .keys(new LinkedHashSet<>(List.of("product_name")))
                        .rows(List.of())
                        .build(),
                    CollectionDataset.builder()
                        .keys(new LinkedHashSet<>(List.of("payment_amount")))
                        .rows(List.of())
                        .build()
                ))
                .build()
        );

        try (XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(generated))) {
            assertThat(document.getTables().getFirst().getRows()).hasSize(1);
            assertThat(document.getParagraphs())
                .extracting(XWPFParagraph::getText)
                .doesNotContain("Платеж ${payment_amount} ${currency}");
        }
    }

    @Test
    @DisplayName("Процессор DOCX падает, если collection placeholder не покрыт dataset или scalar value")
    void givenCollectionPlaceholderWithoutDataset_whenGenerate_thenThrowDetailedError() throws IOException {
        DocxTemplateProcessor systemUnderTest = createProcessor();
        byte[] content = createDocxContentWithMixedCollectionKeys();

        assertThatThrownBy(() -> systemUnderTest.generate(
            content,
            GenerationTemplateContext.builder()
                .scalarValues(Map.of(
                    "currency", "RUB"
                ))
                .collections(List.of(
                    CollectionDataset.builder()
                        .keys(new LinkedHashSet<>(List.of("payment_amount")))
                        .rows(List.of(Map.of("payment_amount", "100")))
                        .build()
                ))
                .build()
        ))
            .isInstanceOf(BusinessCrmException.class)
            .satisfies(throwable -> {
                BusinessCrmException exception = (BusinessCrmException) throwable;
                assertThat(exception.getCode()).isEqualTo(TemplateConstants.ErrorCodes.TEMPLATE_COLLECTION_PLACEHOLDERS_MISSING_DATASET);
                assertThat(exception.getParams()).containsExactly("[missing_collection]");
            });
    }

    @Test
    @DisplayName("Процессор DOCX падает, если collection placeholders неоднозначны между dataset'ами")
    void givenAmbiguousCollectionDatasets_whenGenerate_thenThrowDetailedError() throws IOException {
        DocxTemplateProcessor systemUnderTest = createProcessor();
        byte[] content = createDocxListOnlyContent();

        assertThatThrownBy(() -> systemUnderTest.generate(
            content,
            GenerationTemplateContext.builder()
                .scalarValues(Map.of("currency", "RUB"))
                .collections(List.of(
                    CollectionDataset.builder()
                        .keys(new LinkedHashSet<>(List.of("payment_amount")))
                        .rows(List.of(Map.of("payment_amount", "100")))
                        .build(),
                    CollectionDataset.builder()
                        .keys(new LinkedHashSet<>(List.of("payment_amount", "payment_date")))
                        .rows(List.of(Map.of("payment_amount", "250", "payment_date", "2026-05-12")))
                        .build()
                ))
                .build()
        ))
            .isInstanceOf(BusinessCrmException.class)
            .satisfies(throwable -> {
                BusinessCrmException exception = (BusinessCrmException) throwable;
                assertThat(exception.getCode()).isEqualTo(TemplateConstants.ErrorCodes.TEMPLATE_COLLECTION_PLACEHOLDERS_AMBIGUOUS);
                assertThat(exception.getParams()).containsExactly("[payment_amount]");
            });
    }

    private DocxTemplateProcessor createProcessor() {
        DocTemplateProperties docTemplateProperties = new DocTemplateProperties();
        docTemplateProperties.getTemplate().getVariable().setPlaceholderRegex("\\$\\{([A-Za-z0-9_.$]+)}");
        return new DocxTemplateProcessor(docTemplateProperties);
    }

    private byte[] createDocxContent() throws IOException {
        try (XWPFDocument document = new XWPFDocument();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            document.createParagraph().createRun().setText("Номер сделки ${deal_number}");

            XWPFHeader header = document.createHeader(HeaderFooterType.DEFAULT);
            header.createParagraph().createRun().setText("Header ${header_number}");

            XWPFFooter footer = document.createFooter(HeaderFooterType.DEFAULT);
            footer.createParagraph().createRun().setText("Footer ${footer_number}");

            XWPFTable table = document.createTable(2, 1);
            table.getRow(0).getCell(0).setText("Header");
            table.getRow(1).getCell(0).setText("${product_name}");

            XWPFParagraph listParagraph = document.createParagraph();
            listParagraph.setNumID(BigInteger.ONE);
            listParagraph.createRun().setText("Платеж ${payment_amount} ${currency}");

            document.write(outputStream);
            return outputStream.toByteArray();
        }
    }

    private byte[] createDocxWithRepeatedPlaceholder() throws IOException {
        try (XWPFDocument document = new XWPFDocument();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            document.createParagraph().createRun().setText("Клиент ${client_name}, повтор ${client_name}");
            document.write(outputStream);
            return outputStream.toByteArray();
        }
    }

    private byte[] createDocxListBeforeParagraphContent() throws IOException {
        try (XWPFDocument document = new XWPFDocument();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            XWPFParagraph listParagraph = document.createParagraph();
            listParagraph.setNumID(BigInteger.ONE);
            listParagraph.createRun().setText("Платеж ${payment_amount}");
            document.createParagraph().createRun().setText("Итого");
            document.write(outputStream);
            return outputStream.toByteArray();
        }
    }

    private byte[] createDocxParagraphWithMultipleRuns() throws IOException {
        try (XWPFDocument document = new XWPFDocument();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            XWPFParagraph paragraph = document.createParagraph();
            XWPFRun firstRun = paragraph.createRun();
            firstRun.setFontSize(14);
            firstRun.setText("Клиент ${client");
            paragraph.createRun().setText("_name}");
            document.write(outputStream);
            return outputStream.toByteArray();
        }
    }

    private byte[] createStyledDocxParagraph() throws IOException {
        try (XWPFDocument document = new XWPFDocument();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            XWPFRun run = document.createParagraph().createRun();
            run.setFontSize(18);
            run.setText("Клиент ${client_name}");
            document.write(outputStream);
            return outputStream.toByteArray();
        }
    }

    private byte[] createStyledDocxTable() throws IOException {
        try (XWPFDocument document = new XWPFDocument();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            XWPFTable table = document.createTable(1, 1);
            XWPFParagraph paragraph = table.getRow(0).getCell(0).getParagraphs().getFirst();
            XWPFRun run = paragraph.createRun();
            run.setFontSize(16);
            run.setText("Клиент ${client_name}");
            document.write(outputStream);
            return outputStream.toByteArray();
        }
    }

    private byte[] createDocxTableContent() throws IOException {
        try (XWPFDocument document = new XWPFDocument();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            XWPFTable table = document.createTable(2, 3);
            table.getRow(0).getCell(0).setText("Product");
            table.getRow(0).getCell(1).setText("Qty");
            table.getRow(0).getCell(2).setText("Currency");
            table.getRow(1).getCell(0).setText("${product_name}");
            table.getRow(1).getCell(1).setText("${product_qty}");
            table.getRow(1).getCell(2).setText("${currency}");
            document.write(outputStream);
            return outputStream.toByteArray();
        }
    }

    private byte[] createDocxTableWithRowAndCellProperties() throws IOException {
        try (XWPFDocument document = new XWPFDocument();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            XWPFTable table = document.createTable(2, 1);
            table.getRow(0).getCell(0).setText("Product");
            table.getRow(1).getCtRow().addNewTrPr();
            table.getRow(1).getCell(0).getCTTc().addNewTcPr();
            table.getRow(1).getCell(0).setText("${product_name}");
            document.write(outputStream);
            return outputStream.toByteArray();
        }
    }

    private byte[] createDocxListOnlyContent() throws IOException {
        try (XWPFDocument document = new XWPFDocument();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            XWPFParagraph listParagraph = document.createParagraph();
            listParagraph.setNumID(BigInteger.ONE);
            listParagraph.createRun().setText("Платеж ${payment_amount}");
            document.write(outputStream);
            return outputStream.toByteArray();
        }
    }

    private byte[] createDocxContentWithMixedCollectionKeys() throws IOException {
        try (XWPFDocument document = new XWPFDocument();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            XWPFParagraph listParagraph = document.createParagraph();
            listParagraph.setNumID(BigInteger.ONE);
            listParagraph.createRun().setText("Платеж ${payment_amount} ${missing_collection} ${currency}");
            document.write(outputStream);
            return outputStream.toByteArray();
        }
    }

}
