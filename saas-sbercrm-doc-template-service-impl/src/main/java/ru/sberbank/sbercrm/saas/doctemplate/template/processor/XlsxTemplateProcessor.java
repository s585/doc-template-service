package ru.sberbank.sbercrm.saas.doctemplate.template.processor;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.stereotype.Component;
import ru.sberbank.sbercrm.saas.doctemplate.application.exception.model.BusinessCrmException;
import ru.sberbank.sbercrm.saas.doctemplate.document.model.CollectionDataset;
import ru.sberbank.sbercrm.saas.doctemplate.document.model.GenerationTemplateContext;
import ru.sberbank.sbercrm.saas.doctemplate.template.constant.TemplateConstants;
import ru.sberbank.sbercrm.saas.doctemplate.template.properties.DocTemplateProperties;
import ru.sberbank.sbercrm.saas.doctemplate.template.model.MappingScope;
import ru.sberbank.sbercrm.saas.doctemplate.template.model.TemplateVariableInfo;
import ru.sberbank.sbercrm.saas.doctemplate.template.model.TemplateFormat;
import ru.sberbank.sbercrm.saas.doctemplate.template.util.TemplateVariableUtils;

@Component
@RequiredArgsConstructor
public class XlsxTemplateProcessor implements FormatAwareTemplateProcessor {
    private final DocTemplateProperties docTemplateProperties;

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
                            TemplateVariableUtils.extractVariables(cell.toString(), placeholderPattern, MappingScope.COLLECTION)
                        );
                    }
                }
            }
            return occurrences;
        } catch (IOException ex) {
            throw new BusinessCrmException(
                TemplateConstants.ErrorCodes.TEMPLATE_PARSING_FAILED,
                TemplateConstants.ErrorCodes.TEMPLATE_PARSING_FAILED,
                ex,
                TemplateFormat.XLSX.value()
            );
        }
    }

    @Override
    public byte[] generate(byte[] content, GenerationTemplateContext context) {
        try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(content));
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            for (Sheet sheet : workbook) {
                processSheet(sheet, context);
            }
            workbook.write(outputStream);
            return outputStream.toByteArray();
        } catch (IOException ex) {
            throw new BusinessCrmException(
                TemplateConstants.ErrorCodes.TEMPLATE_PARSING_FAILED,
                TemplateConstants.ErrorCodes.TEMPLATE_PARSING_FAILED,
                ex,
                TemplateFormat.XLSX.value()
            );
        }
    }

    private Pattern getPlaceholderPattern() {
        return TemplateVariableUtils.compilePlaceholderPattern(
            docTemplateProperties.getTemplate().getVariable().getPlaceholderRegex()
        );
    }

    private void processSheet(Sheet sheet, GenerationTemplateContext context) {
        Pattern placeholderPattern = getPlaceholderPattern();
        int rowIndex = sheet.getFirstRowNum();
        while (rowIndex <= sheet.getLastRowNum()) {
            Row row = sheet.getRow(rowIndex);
            if (row == null) {
                rowIndex++;
                continue;
            }

            Set<String> placeholders = extractRowPlaceholders(row, placeholderPattern);
            CollectionDataset dataset = resolveCollectionDataset(placeholders, context);
            if (dataset == null) {
                replaceRowValues(row, context.getScalarValues(), placeholderPattern);
                rowIndex++;
                continue;
            }

            int repeatCount = dataset.getRows().size();
            RowSnapshot snapshot = captureRowSnapshot(row);
            if (repeatCount == 0) {
                removeRow(sheet, rowIndex);
                continue;
            }

            if (repeatCount > 1 && rowIndex < sheet.getLastRowNum()) {
                sheet.shiftRows(rowIndex + 1, sheet.getLastRowNum(), repeatCount - 1, true, false);
            }
            for (int itemIndex = 0; itemIndex < repeatCount; itemIndex++) {
                Row targetRow = itemIndex == 0 ? getOrCreateRow(sheet, rowIndex) : sheet.createRow(rowIndex + itemIndex);
                copyRowSnapshot(
                    targetRow,
                    snapshot,
                    buildRowValues(placeholders, context, dataset, itemIndex),
                    placeholderPattern
                );
            }
            rowIndex += repeatCount;
        }
    }

    private Set<String> extractRowPlaceholders(Row row, Pattern placeholderPattern) {
        Set<String> placeholders = new LinkedHashSet<>();
        for (Cell cell : row) {
            placeholders.addAll(
                TemplateVariableUtils.extractVariables(cell.toString(), placeholderPattern, MappingScope.COLLECTION)
                    .stream()
                    .map(TemplateVariableInfo::getKey)
                    .collect(Collectors.toCollection(LinkedHashSet::new))
            );
        }
        return placeholders;
    }

    private CollectionDataset resolveCollectionDataset(Set<String> placeholders, GenerationTemplateContext context) {
        Set<String> datasetKeys = context.getCollections().stream()
            .flatMap(dataset -> dataset.getKeys().stream())
            .collect(Collectors.toCollection(LinkedHashSet::new));
        List<String> collectionKeys = placeholders.stream()
            .filter(datasetKeys::contains)
            .toList();
        if (collectionKeys.isEmpty()) {
            return null;
        }
        List<String> unresolvedKeys = placeholders.stream()
            .filter(key -> !collectionKeys.contains(key))
            .filter(key -> !context.getScalarValues().containsKey(key))
            .toList();
        if (!unresolvedKeys.isEmpty()) {
            throw new BusinessCrmException(
                TemplateConstants.ErrorCodes.TEMPLATE_COLLECTION_PLACEHOLDERS_MISSING_DATASET,
                TemplateConstants.ErrorCodes.TEMPLATE_COLLECTION_PLACEHOLDERS_MISSING_DATASET,
                unresolvedKeys.toString()
            );
        }
        List<CollectionDataset> matchingDatasets = context.getCollections().stream()
            .filter(dataset -> dataset.getKeys().containsAll(collectionKeys))
            .toList();
        if (matchingDatasets.isEmpty()) {
            throw new BusinessCrmException(
                TemplateConstants.ErrorCodes.TEMPLATE_COLLECTION_PLACEHOLDERS_MISSING_DATASET,
                TemplateConstants.ErrorCodes.TEMPLATE_COLLECTION_PLACEHOLDERS_MISSING_DATASET,
                collectionKeys.toString()
            );
        }
        if (matchingDatasets.size() > 1) {
            throw new BusinessCrmException(
                TemplateConstants.ErrorCodes.TEMPLATE_COLLECTION_PLACEHOLDERS_AMBIGUOUS,
                TemplateConstants.ErrorCodes.TEMPLATE_COLLECTION_PLACEHOLDERS_AMBIGUOUS,
                collectionKeys.toString()
            );
        }
        return matchingDatasets.getFirst();
    }

    private Map<String, String> buildRowValues(
        Set<String> placeholders,
        GenerationTemplateContext context,
        CollectionDataset dataset,
        int itemIndex
    ) {
        Map<String, String> rowValues = new HashMap<>(context.getScalarValues());
        Map<String, String> datasetRow = dataset.getRows().get(itemIndex);
        for (String placeholder : placeholders) {
            if (dataset.getKeys().contains(placeholder)) {
                rowValues.put(placeholder, datasetRow.getOrDefault(placeholder, ""));
            }
        }
        return rowValues;
    }

    private void replaceRowValues(Row row, Map<String, String> values, Pattern placeholderPattern) {
        for (Cell cell : row) {
            if (cell.getCellType() == CellType.STRING) {
                cell.setCellValue(
                    TemplateVariableUtils.replacePlaceholders(
                        cell.getStringCellValue(),
                        values,
                        placeholderPattern
                    )
                );
            }
        }
    }

    private RowSnapshot captureRowSnapshot(Row row) {
        List<CellSnapshot> cells = new ArrayList<>();
        for (Cell cell : row) {
            cells.add(CellSnapshot.from(cell));
        }
        return new RowSnapshot(row.getHeight(), row.getZeroHeight(), row.getRowStyle(), cells);
    }

    private void copyRowSnapshot(
        Row targetRow,
        RowSnapshot snapshot,
        Map<String, String> values,
        Pattern placeholderPattern
    ) {
        targetRow.setHeight(snapshot.height());
        targetRow.setZeroHeight(snapshot.zeroHeight());
        if (snapshot.style() != null) {
            targetRow.setRowStyle(snapshot.style());
        }
        removeAllCells(targetRow);
        for (CellSnapshot cellSnapshot : snapshot.cells()) {
            Cell targetCell = targetRow.createCell(cellSnapshot.columnIndex(), cellSnapshot.type());
            if (cellSnapshot.style() != null) {
                targetCell.setCellStyle(cellSnapshot.style());
            }
            switch (cellSnapshot.type()) {
                case STRING -> targetCell.setCellValue(
                    TemplateVariableUtils.replacePlaceholders(cellSnapshot.stringValue(), values, placeholderPattern)
                );
                case NUMERIC -> targetCell.setCellValue(cellSnapshot.numericValue());
                case BOOLEAN -> targetCell.setCellValue(cellSnapshot.booleanValue());
                case FORMULA -> targetCell.setCellFormula(cellSnapshot.formula());
                case BLANK -> targetCell.setBlank();
                default -> targetCell.setCellValue(cellSnapshot.stringValue());
            }
        }
    }

    private void removeRow(Sheet sheet, int rowIndex) {
        Row row = sheet.getRow(rowIndex);
        if (row == null) {
            return;
        }
        if (rowIndex < sheet.getLastRowNum()) {
            sheet.shiftRows(rowIndex + 1, sheet.getLastRowNum(), -1, true, false);
        } else {
            sheet.removeRow(row);
        }
    }

    private void removeAllCells(Row row) {
        for (int cellIndex = row.getLastCellNum() - 1; cellIndex >= 0; cellIndex--) {
            Cell cell = row.getCell(cellIndex);
            if (cell != null) {
                row.removeCell(cell);
            }
        }
    }

    private Row getOrCreateRow(Sheet sheet, int rowIndex) {
        Row row = sheet.getRow(rowIndex);
        return row == null ? sheet.createRow(rowIndex) : row;
    }

    private record RowSnapshot(
        short height,
        boolean zeroHeight,
        CellStyle style,
        List<CellSnapshot> cells
    ) {
    }

    private record CellSnapshot(
        int columnIndex,
        CellType type,
        String stringValue,
        Double numericValue,
        Boolean booleanValue,
        String formula,
        CellStyle style
    ) {
        private static CellSnapshot from(Cell cell) {
            return switch (cell.getCellType()) {
                case STRING -> new CellSnapshot(
                    cell.getColumnIndex(),
                    CellType.STRING,
                    cell.getStringCellValue(),
                    null,
                    null,
                    null,
                    cell.getCellStyle()
                );
                case NUMERIC -> new CellSnapshot(
                    cell.getColumnIndex(),
                    CellType.NUMERIC,
                    null,
                    cell.getNumericCellValue(),
                    null,
                    null,
                    cell.getCellStyle()
                );
                case BOOLEAN -> new CellSnapshot(
                    cell.getColumnIndex(),
                    CellType.BOOLEAN,
                    null,
                    null,
                    cell.getBooleanCellValue(),
                    null,
                    cell.getCellStyle()
                );
                case FORMULA -> new CellSnapshot(
                    cell.getColumnIndex(),
                    CellType.FORMULA,
                    null,
                    null,
                    null,
                    cell.getCellFormula(),
                    cell.getCellStyle()
                );
                case BLANK -> new CellSnapshot(
                    cell.getColumnIndex(),
                    CellType.BLANK,
                    "",
                    null,
                    null,
                    null,
                    cell.getCellStyle()
                );
                default -> new CellSnapshot(
                    cell.getColumnIndex(),
                    CellType.STRING,
                    cell.toString(),
                    null,
                    null,
                    null,
                    cell.getCellStyle()
                );
            };
        }
    }
}
