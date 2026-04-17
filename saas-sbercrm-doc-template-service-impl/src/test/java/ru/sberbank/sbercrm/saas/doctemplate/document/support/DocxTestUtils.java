package ru.sberbank.sbercrm.saas.doctemplate.document.support;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import org.apache.poi.xwpf.usermodel.XWPFDocument;

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
