package ru.sberbank.sbercrm.saas.doctemplate.document.support;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.math.BigInteger;
import java.util.List;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFTable;

public final class DocxTestUtils {
    private DocxTestUtils() {
    }

    public static byte[] createDocx(String text) throws Exception {
        try (
            XWPFDocument document = new XWPFDocument();
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream()
        ) {
            document.createParagraph().createRun().setText(text);
            document.write(outputStream);
            return outputStream.toByteArray();
        }
    }

    public static byte[] createDocxListItem(String text) throws Exception {
        try (
            XWPFDocument document = new XWPFDocument();
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream()
        ) {
            XWPFParagraph paragraph = document.createParagraph();
            paragraph.setNumID(BigInteger.ONE);
            paragraph.createRun().setText(text);
            document.write(outputStream);
            return outputStream.toByteArray();
        }
    }

    public static byte[] createDocxTable(List<String> headerCells, List<String> templateCells) throws Exception {
        try (
            XWPFDocument document = new XWPFDocument();
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream()
        ) {
            XWPFTable table = document.createTable(2, headerCells.size());
            for (int cellIndex = 0; cellIndex < headerCells.size(); cellIndex++) {
                table.getRow(0).getCell(cellIndex).setText(headerCells.get(cellIndex));
                table.getRow(1).getCell(cellIndex).setText(templateCells.get(cellIndex));
            }
            document.write(outputStream);
            return outputStream.toByteArray();
        }
    }

    public static String readDocxText(byte[] content) throws Exception {
        try (
            ByteArrayInputStream inputStream = new ByteArrayInputStream(content);
            XWPFDocument document = new XWPFDocument(inputStream)
        ) {
            return document.getParagraphs().stream()
                .map(paragraph -> paragraph.getText() == null ? "" : paragraph.getText())
                .reduce("", String::concat);
        }
    }
}
