package ru.sberbank.sbercrm.saas.doctemplate.document.model;

import lombok.Builder;

@Builder
public record GenerationArtifactMeta(
    String s3Key,
    String checksum,
    Long sizeBytes
) {
}
