package ru.sberbank.sbercrm.saas.doctemplate.template.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import ru.sberbank.sbercrm.saas.doctemplate.application.exception.CrmErrorCodes;
import ru.sberbank.sbercrm.saas.doctemplate.template.constant.TemplateConstants;
import org.springframework.web.multipart.MultipartFile;
import ru.sberbank.sbercrm.saas.doctemplate.application.exception.model.BusinessCrmException;
import ru.sberbank.sbercrm.saas.doctemplate.template.model.TemplateFormat;

import java.io.IOException;
import java.util.Locale;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class TemplateFileUtils {
    public static byte[] readBytes(MultipartFile file) {
        try {
            return file.getBytes();
        } catch (IOException ex) {
            throw new BusinessCrmException(ex, TemplateConstants.ErrorCodes.TEMPLATE_FILE_INVALID);
        }
    }

    public static String resolveOriginalFileName(MultipartFile file) {
        String originalFileName = file.getOriginalFilename();
        if (originalFileName == null || originalFileName.isBlank()) {
            throw new BusinessCrmException(TemplateConstants.ErrorCodes.TEMPLATE_FILE_INVALID);
        }
        return originalFileName;
    }

    public static TemplateFormat resolveFormat(String originalFileName) {
        int extensionIndex = originalFileName.lastIndexOf('.');
        if (extensionIndex < 0 || extensionIndex == originalFileName.length() - 1) {
            throw new BusinessCrmException(TemplateConstants.ErrorCodes.TEMPLATE_FILE_INVALID);
        }

        String extension = originalFileName.substring(extensionIndex + 1).toLowerCase(Locale.ROOT);
        return switch (extension) {
            case "docx" -> TemplateFormat.DOCX;
            case "xlsx" -> TemplateFormat.XLSX;
            default -> throw new BusinessCrmException(TemplateConstants.ErrorCodes.TEMPLATE_FORMAT_UNSUPPORTED, extension);
        };
    }
}
