package ru.sberbank.sbercrm.saas.doctemplate.document.model;

import lombok.Builder;

@Builder
public record GeneratedFileResult(
    String s3Key,
    String checksum,
    long sizeBytes
) {
}
