package ru.sberbank.sbercrm.saas.doctemplate.template.processor;

import lombok.RequiredArgsConstructor;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.openxml4j.exceptions.NotOfficeXmlFileException;
import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.springframework.stereotype.Component;
import ru.sberbank.sbercrm.saas.doctemplate.application.exception.model.BusinessCrmException;
import ru.sberbank.sbercrm.saas.doctemplate.document.model.CollectionDataset;
import ru.sberbank.sbercrm.saas.doctemplate.document.model.GenerationTemplateContext;
import ru.sberbank.sbercrm.saas.doctemplate.template.constant.TemplateConstants;
import ru.sberbank.sbercrm.saas.doctemplate.template.properties.DocTemplateProperties;
import ru.sberbank.sbercrm.saas.doctemplate.template.model.MappingScope;
import ru.sberbank.sbercrm.saas.doctemplate.template.model.TemplateFormat;
import ru.sberbank.sbercrm.saas.doctemplate.template.model.TemplateVariableInfo;
import ru.sberbank.sbercrm.saas.doctemplate.template.util.TemplateVariableUtils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.regex.Pattern;
import org.apache.poi.xwpf.usermodel.IBodyElement;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTcPr;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTPPr;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTRPr;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTrPr;
import org.apache.xmlbeans.XmlCursor;

@Component
@RequiredArgsConstructor
public class DocxTemplateProcessor implements FormatAwareTemplateProcessor {
    private final DocTemplateProperties docTemplateProperties;

    @Override
    public boolean supports(TemplateFormat format) {
        return TemplateFormat.DOCX == format;
    }

    @Override
    public List<TemplateVariableInfo> extractVariables(byte[] content) {
        try (XWPFDocument document = new XWPFDocument(OPCPackage.open(new ByteArrayInputStream(content)))) {
            Pattern placeholderPattern = getPlaceholderPattern();
            List<TemplateVariableInfo> variables = new ArrayList<>();
            int[] blockNumber = {1};
            for (IBodyElement bodyElement : document.getBodyElements()) {
                if (bodyElement instanceof XWPFParagraph paragraph) {
                    variables.addAll(extractParagraphVariables(paragraph, placeholderPattern, blockNumber));
                    continue;
                }
                if (bodyElement instanceof XWPFTable table) {
                    variables.addAll(extractTableVariables(table, placeholderPattern, blockNumber));
                }
            }
            if (document.getHeaderList() != null) {
                document.getHeaderList().forEach(
                    header -> variables.addAll(
                        TemplateVariableUtils.extractVariables(header.getText(), placeholderPattern, MappingScope.VALUE)
                    )
                );
            }
            if (document.getFooterList() != null) {
                document.getFooterList().forEach(
                    footer -> variables.addAll(
                        TemplateVariableUtils.extractVariables(footer.getText(), placeholderPattern, MappingScope.VALUE)
                    )
                );
            }
            return variables;
        } catch (IOException | InvalidFormatException | NotOfficeXmlFileException ex) {
            throw new BusinessCrmException(
                TemplateConstants.ErrorCodes.TEMPLATE_PARSING_FAILED,
                TemplateConstants.ErrorCodes.TEMPLATE_PARSING_FAILED,
                ex,
                TemplateFormat.DOCX.value()
            );
        }
    }

    @Override
    public byte[] generate(byte[] content, GenerationTemplateContext context) {
        try (XWPFDocument document = new XWPFDocument(OPCPackage.open(new ByteArrayInputStream(content)));
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            processBodyElements(document, context);
            if (document.getHeaderList() != null) {
                document.getHeaderList().forEach(
                    header -> header.getParagraphs().forEach(
                        paragraph -> replaceParagraphText(paragraph, context.getScalarValues())
                    )
                );
            }
            if (document.getFooterList() != null) {
                document.getFooterList().forEach(
                    footer -> footer.getParagraphs().forEach(
                        paragraph -> replaceParagraphText(paragraph, context.getScalarValues())
                    )
                );
            }
            document.write(outputStream);
            return outputStream.toByteArray();
        } catch (IOException | InvalidFormatException | NotOfficeXmlFileException ex) {
            throw new BusinessCrmException(
                TemplateConstants.ErrorCodes.TEMPLATE_PARSING_FAILED,
                TemplateConstants.ErrorCodes.TEMPLATE_PARSING_FAILED,
                ex,
                TemplateFormat.DOCX.value()
            );
        }
    }

    private Pattern getPlaceholderPattern() {
        return TemplateVariableUtils.compilePlaceholderPattern(
            docTemplateProperties.getTemplate().getVariable().getPlaceholderRegex()
        );
    }

    private List<TemplateVariableInfo> extractParagraphVariables(
        XWPFParagraph paragraph,
        Pattern placeholderPattern,
        int[] blockNumber
    ) {
        MappingScope scope = paragraph.getNumID() == null ? MappingScope.VALUE : MappingScope.COLLECTION;
        List<TemplateVariableInfo> variables = TemplateVariableUtils.extractVariables(
            paragraph.getText(),
            placeholderPattern,
            scope
        );
        if (scope == MappingScope.COLLECTION) {
            assignBlockId(variables, "docx", blockNumber);
        }
        return variables;
    }

    private List<TemplateVariableInfo> extractTableVariables(
        XWPFTable table,
        Pattern placeholderPattern,
        int[] blockNumber
    ) {
        List<TemplateVariableInfo> variables = new ArrayList<>();
        for (XWPFTableRow row : table.getRows()) {
            List<TemplateVariableInfo> rowVariables = new ArrayList<>();
            for (XWPFTableCell cell : row.getTableCells()) {
                rowVariables.addAll(
                    TemplateVariableUtils.extractVariables(cell.getText(), placeholderPattern, MappingScope.COLLECTION)
                );
            }
            assignBlockId(rowVariables, "docx", blockNumber);
            variables.addAll(rowVariables);
        }
        return variables;
    }

    private void assignBlockId(List<TemplateVariableInfo> variables, String formatPrefix, int[] blockNumber) {
        if (variables.isEmpty()) {
            return;
        }
        String blockId = formatBlockId(formatPrefix, blockNumber[0]++, variables.getFirst().getKey());
        variables.forEach(variable -> variable.setBlockId(blockId));
    }

    private String formatBlockId(String formatPrefix, int blockNumber, String anchorKey) {
        return String.format("%s:block:%03d:%s", formatPrefix, blockNumber, anchorKey);
    }

    private void processBodyElements(XWPFDocument document, GenerationTemplateContext context) {
        int bodyElementIndex = 0;
        while (bodyElementIndex < document.getBodyElements().size()) {
            IBodyElement bodyElement = document.getBodyElements().get(bodyElementIndex);
            if (bodyElement instanceof XWPFParagraph paragraph) {
                if (paragraph.getNumID() != null) {
                    bodyElementIndex = processListParagraph(document, bodyElementIndex, paragraph, context);
                } else {
                    replaceParagraphText(paragraph, context.getScalarValues());
                    bodyElementIndex++;
                }
                continue;
            }
            if (bodyElement instanceof XWPFTable table) {
                processTable(table, context);
            }
            bodyElementIndex++;
        }
    }

    private void processTable(XWPFTable table, GenerationTemplateContext context) {
        int rowIndex = 0;
        while (rowIndex < table.getRows().size()) {
            XWPFTableRow row = table.getRow(rowIndex);
            Set<String> placeholders = extractRowPlaceholders(row);
            CollectionDataset dataset = CollectionTemplateProcessorSupport.resolveCollectionDataset(placeholders, context);
            if (dataset == null) {
                for (XWPFTableCell cell : row.getTableCells()) {
                    for (XWPFParagraph paragraph : cell.getParagraphs()) {
                        replaceParagraphText(paragraph, context.getScalarValues());
                    }
                }
                rowIndex++;
                continue;
            }

            int repeatCount = dataset.getRows().size();
            for (int itemIndex = 0; itemIndex < repeatCount; itemIndex++) {
                XWPFTableRow insertedRow = table.insertNewTableRow(rowIndex + itemIndex);
                copyRowTemplate(insertedRow, row, buildRowValues(placeholders, context, dataset, itemIndex));
            }
            table.removeRow(rowIndex + repeatCount);
            rowIndex += repeatCount;
        }
    }

    private int processListParagraph(
        XWPFDocument document,
        int bodyElementIndex,
        XWPFParagraph paragraph,
        GenerationTemplateContext context
    ) {
        Set<String> placeholders = extractParagraphPlaceholders(paragraph, MappingScope.COLLECTION);
        CollectionDataset dataset = CollectionTemplateProcessorSupport.resolveCollectionDataset(placeholders, context);
        if (dataset == null) {
            replaceParagraphText(paragraph, context.getScalarValues());
            return bodyElementIndex + 1;
        }

        int repeatCount = dataset.getRows().size();
        for (int itemIndex = 0; itemIndex < repeatCount; itemIndex++) {
            XWPFParagraph insertedParagraph = insertParagraphAt(document, bodyElementIndex + itemIndex + 1);
            copyParagraphTemplate(insertedParagraph, paragraph, buildRowValues(placeholders, context, dataset, itemIndex));
        }
        document.removeBodyElement(bodyElementIndex);
        return bodyElementIndex + repeatCount;
    }

    private Set<String> extractRowPlaceholders(XWPFTableRow row) {
        Set<String> placeholders = new LinkedHashSet<>();
        for (XWPFTableCell cell : row.getTableCells()) {
            for (XWPFParagraph paragraph : cell.getParagraphs()) {
                placeholders.addAll(extractParagraphPlaceholders(paragraph, MappingScope.COLLECTION));
            }
        }
        return placeholders;
    }

    private Set<String> extractParagraphPlaceholders(XWPFParagraph paragraph, MappingScope scope) {
        return TemplateVariableUtils.extractVariables(paragraph.getText(), getPlaceholderPattern(), scope)
            .stream()
            .map(TemplateVariableInfo::getKey)
            .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private Map<String, String> buildRowValues(
        Set<String> placeholders,
        GenerationTemplateContext context,
        CollectionDataset dataset,
        int itemIndex
    ) {
        return CollectionTemplateProcessorSupport.buildRowValues(placeholders, context, dataset, itemIndex);
    }

    private void copyRowTemplate(XWPFTableRow targetRow, XWPFTableRow templateRow, Map<String, String> values) {
        if (templateRow.getCtRow().getTrPr() != null) {
            targetRow.getCtRow().setTrPr((CTTrPr) templateRow.getCtRow().getTrPr().copy());
        }
        for (XWPFTableCell templateCell : templateRow.getTableCells()) {
            XWPFTableCell targetCell = targetRow.addNewTableCell();
            if (templateCell.getCTTc().getTcPr() != null) {
                targetCell.getCTTc().setTcPr((CTTcPr) templateCell.getCTTc().getTcPr().copy());
            }
            copyCellTemplate(templateCell, targetCell, values);
        }
    }

    private XWPFParagraph insertParagraphAt(XWPFDocument document, int bodyInsertIndex) {
        if (bodyInsertIndex >= document.getBodyElements().size()) {
            return document.createParagraph();
        }
        try (XmlCursor cursor = getBodyElementCursor(document.getBodyElements().get(bodyInsertIndex))) {
            return document.insertNewParagraph(cursor);
        }
    }

    private XmlCursor getBodyElementCursor(IBodyElement bodyElement) {
        if (bodyElement instanceof XWPFParagraph paragraph) {
            return paragraph.getCTP().newCursor();
        }
        if (bodyElement instanceof XWPFTable table) {
            return table.getCTTbl().newCursor();
        }
        throw new BusinessCrmException(
            TemplateConstants.ErrorCodes.TEMPLATE_COLLECTION_UNSUPPORTED_BODY_ELEMENT,
            TemplateConstants.ErrorCodes.TEMPLATE_COLLECTION_UNSUPPORTED_BODY_ELEMENT,
            bodyElement.getElementType()
        );
    }

    private void copyParagraphTemplate(XWPFParagraph targetParagraph, XWPFParagraph templateParagraph, Map<String, String> values) {
        if (templateParagraph.getCTP().getPPr() != null) {
            targetParagraph.getCTP().setPPr((CTPPr) templateParagraph.getCTP().getPPr().copy());
        }
        String paragraphText = TemplateVariableUtils.replacePlaceholders(
            templateParagraph.getText(),
            values,
            getPlaceholderPattern()
        );
        replaceRunsPreservingFirstRunStyle(targetParagraph, templateParagraph, paragraphText);
    }

    private void replaceParagraphText(XWPFParagraph paragraph, Map<String, String> values) {
        String sourceText = paragraph.getText();
        if (sourceText == null || sourceText.isBlank()) {
            return;
        }
        String replacedText = TemplateVariableUtils.replacePlaceholders(sourceText, values, getPlaceholderPattern());
        if (sourceText.equals(replacedText)) {
            return;
        }
        replaceRunsPreservingFirstRunStyle(paragraph, paragraph, replacedText);
    }

    private void copyCellTemplate(XWPFTableCell sourceCell, XWPFTableCell targetCell, Map<String, String> values) {
        for (int i = targetCell.getParagraphs().size() - 1; i >= 0; i--) {
            targetCell.removeParagraph(i);
        }
        for (XWPFParagraph sourceParagraph : sourceCell.getParagraphs()) {
            XWPFParagraph targetParagraph = targetCell.addParagraph();
            copyParagraphTemplate(targetParagraph, sourceParagraph, values);
        }
    }

    private void replaceRunsPreservingFirstRunStyle(
        XWPFParagraph targetParagraph,
        XWPFParagraph styleSourceParagraph,
        String text
    ) {
        XWPFRun styleSourceRun = styleSourceParagraph.getRuns().isEmpty()
            ? null
            : styleSourceParagraph.getRuns().getFirst();
        XWPFRun targetRun;
        if (targetParagraph.getRuns().isEmpty()) {
            targetRun = targetParagraph.createRun();
        } else {
            targetRun = targetParagraph.getRuns().getFirst();
            targetRun.setText("", 0);
        }
        copyRunStyle(targetRun, styleSourceRun);
        targetRun.setText(text, 0);
        for (int i = targetParagraph.getRuns().size() - 1; i > 0; i--) {
            targetParagraph.removeRun(i);
        }
    }

    private void copyRunStyle(XWPFRun targetRun, XWPFRun sourceRun) {
        if (sourceRun == null || sourceRun.getCTR().getRPr() == null) {
            return;
        }
        targetRun.getCTR().setRPr((CTRPr) sourceRun.getCTR().getRPr().copy());
    }
}
