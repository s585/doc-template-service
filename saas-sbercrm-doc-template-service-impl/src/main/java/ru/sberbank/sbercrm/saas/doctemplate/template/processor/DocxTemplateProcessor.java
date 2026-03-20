package ru.sberbank.sbercrm.saas.doctemplate.template.processor;

import lombok.RequiredArgsConstructor;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.springframework.stereotype.Component;
import ru.sberbank.sbercrm.saas.doctemplate.application.exception.CrmErrorCodes;
import ru.sberbank.sbercrm.saas.doctemplate.template.constant.TemplateConstants;
import ru.sberbank.sbercrm.saas.doctemplate.application.exception.model.BusinessCrmException;
import ru.sberbank.sbercrm.saas.doctemplate.template.properties.TemplateProperties;
import ru.sberbank.sbercrm.saas.doctemplate.template.model.MappingScope;
import ru.sberbank.sbercrm.saas.doctemplate.template.model.TemplateFormat;
import ru.sberbank.sbercrm.saas.doctemplate.template.model.TemplateVariableInfo;
import ru.sberbank.sbercrm.saas.doctemplate.template.util.TemplateVariableUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@Component
@RequiredArgsConstructor
public class DocxTemplateProcessor implements FormatAwareTemplateProcessor {
    private final TemplateProperties templateProperties;

    @Override
    public boolean supports(TemplateFormat format) {
        return TemplateFormat.DOCX == format;
    }

    @Override
    public List<TemplateVariableInfo> extractVariables(byte[] content) {
        try (XWPFDocument document = new XWPFDocument(OPCPackage.open(new ByteArrayInputStream(content)))) {
            Pattern placeholderPattern = getPlaceholderPattern();
            List<TemplateVariableInfo> occurrences = new ArrayList<>();
            for (XWPFParagraph paragraph : document.getParagraphs()) {
                occurrences.addAll(
                    TemplateVariableUtils.extractOccurrences(paragraph.getText(), placeholderPattern, MappingScope.VALUE)
                );
            }
            for (XWPFTable table : document.getTables()) {
                for (XWPFTableRow row : table.getRows()) {
                    for (XWPFTableCell cell : row.getTableCells()) {
                        occurrences.addAll(
                            TemplateVariableUtils.extractOccurrences(cell.getText(), placeholderPattern, MappingScope.TABLE)
                        );
                    }
                }
            }
            if (document.getHeaderList() != null) {
                document.getHeaderList().forEach(
                    header -> occurrences.addAll(
                        TemplateVariableUtils.extractOccurrences(header.getText(), placeholderPattern, MappingScope.VALUE)
                    )
                );
            }
            if (document.getFooterList() != null) {
                document.getFooterList().forEach(
                    footer -> occurrences.addAll(
                        TemplateVariableUtils.extractOccurrences(footer.getText(), placeholderPattern, MappingScope.VALUE)
                    )
                );
            }
            return occurrences;
        } catch (IOException | InvalidFormatException ex) {
            throw new BusinessCrmException(ex, TemplateConstants.ErrorCodes.TEMPLATE_PARSING_FAILED, TemplateFormat.DOCX.value());
        }
    }

    private Pattern getPlaceholderPattern() {
        return TemplateVariableUtils.compilePlaceholderPattern(
            templateProperties.getTemplate().getVariable().getPlaceholderRegex()
        );
    }
}
