package ru.sberbank.sbercrm.saas.doctemplate.document.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonPropertyOrder({
    "id",
    "format",
    "status",
    "s3Key",
    "checksum",
    "sizeBytes",
    "errorCode",
    "errorMessage",
    "createdAt",
    "updatedAt"
})
@Schema(title = "Файл документа")
public class GeneratedFileRs {

    @NotNull
    @Schema(title = "Идентификатор файла документа")
    private UUID id;

    @NotNull
    @Schema(title = "Формат файла")
    private String format;

    @NotNull
    @Schema(title = "Статус файла")
    private String status;

    @Schema(title = "Ключ файла в S3")
    @Nullable
    private String s3Key;

    @Schema(title = "Контрольная сумма файла")
    @Nullable
    private String checksum;

    @Schema(title = "Размер файла в байтах")
    @Nullable
    private Long sizeBytes;

    @Schema(title = "Код ошибки файла")
    @Nullable
    private String errorCode;

    @Schema(title = "Текст ошибки файла")
    @Nullable
    private String errorMessage;

    @NotNull
    @Schema(title = "Дата и время создания файла", example = "2026-04-02T10:15:00+03:00")
    private OffsetDateTime createdAt;

    @NotNull
    @Schema(title = "Дата и время обновления файла", example = "2026-04-02T10:16:10+03:00")
    private OffsetDateTime updatedAt;
}
