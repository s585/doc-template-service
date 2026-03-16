package ru.sberbank.sbercrm.doctemplate.template.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import ru.sberbank.sbercrm.doctemplate.common.constant.CrmErrorCodes;
import org.springframework.web.multipart.MultipartFile;
import ru.sberbank.sbercrm.doctemplate.common.exception.BusinessCrmException;
import ru.sberbank.sbercrm.doctemplate.template.model.TemplateFormat;

import java.io.IOException;
import java.util.Locale;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class TemplateFileUtils {
    public static byte[] readBytes(MultipartFile file) {
        try {
            return file.getBytes();
        } catch (IOException ex) {
            throw new BusinessCrmException(ex, CrmErrorCodes.TEMPLATE_FILE_INVALID);
        }
    }

    public static String resolveOriginalFileName(MultipartFile file) {
        String originalFileName = file.getOriginalFilename();
        if (originalFileName == null || originalFileName.isBlank()) {
            throw new BusinessCrmException(CrmErrorCodes.TEMPLATE_FILE_INVALID);
        }
        return originalFileName;
    }

    public static TemplateFormat resolveFormat(String originalFileName) {
        int extensionIndex = originalFileName.lastIndexOf('.');
        if (extensionIndex < 0 || extensionIndex == originalFileName.length() - 1) {
            throw new BusinessCrmException(CrmErrorCodes.TEMPLATE_FILE_INVALID, originalFileName);
        }

        String extension = originalFileName.substring(extensionIndex + 1).toLowerCase(Locale.ROOT);
        return switch (extension) {
            case "docx" -> TemplateFormat.DOCX;
            case "xlsx" -> TemplateFormat.XLSX;
            default -> throw new BusinessCrmException(CrmErrorCodes.TEMPLATE_FORMAT_UNSUPPORTED, extension);
        };
    }
}
