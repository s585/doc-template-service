package ru.sberbank.sbercrm.saas.doctemplate.template.processor;

import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
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
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

@Component
@RequiredArgsConstructor
public class XlsxTemplateProcessor implements FormatAwareTemplateProcessor {
    private final TemplateProperties templateProperties;

    @Override
    public boolean supports(TemplateFormat format) {
        return TemplateFormat.XLSX == format;
    }

    @Override
    public List<TemplateVariableInfo> extractVariables(byte[] content) {
        try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(content))) {
            Pattern placeholderPattern = getPlaceholderPattern();
            List<TemplateVariableInfo> occurrences = new ArrayList<>();
            for (Sheet sheet : workbook) {
                for (Row row : sheet) {
                    for (Cell cell : row) {
                        occurrences.addAll(
                            TemplateVariableUtils.extractOccurrences(cell.toString(), placeholderPattern, MappingScope.TABLE)
                        );
                    }
                }
            }
            return occurrences;
        } catch (IOException ex) {
            throw new BusinessCrmException(ex, TemplateConstants.ErrorCodes.TEMPLATE_PARSING_FAILED, TemplateFormat.XLSX.value());
        }
    }

    @Override
    public byte[] generate(byte[] content, Map<String, String> values) {
        try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(content));
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            for (Sheet sheet : workbook) {
                for (Row row : sheet) {
                    for (Cell cell : row) {
                        if (cell.getCellType() == org.apache.poi.ss.usermodel.CellType.STRING) {
                            cell.setCellValue(applyValues(cell.getStringCellValue(), values));
                        }
                    }
                }
            }
            workbook.write(outputStream);
            return outputStream.toByteArray();
        } catch (IOException ex) {
            throw new BusinessCrmException(ex, TemplateConstants.ErrorCodes.TEMPLATE_PARSING_FAILED, TemplateFormat.XLSX.value());
        }
    }

    private Pattern getPlaceholderPattern() {
        return TemplateVariableUtils.compilePlaceholderPattern(
            templateProperties.getTemplate().getVariable().getPlaceholderRegex()
        );
    }

    private String applyValues(String sourceText, Map<String, String> values) {
        String result = sourceText;
        for (Map.Entry<String, String> entry : values.entrySet()) {
            result = result.replace("${" + entry.getKey() + "}", entry.getValue());
        }
        return result;
    }
}
