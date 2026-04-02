package ru.sberbank.sbercrm.saas.doctemplate.document.model;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class DocumentCreationCmd {
    private UUID templateId;
    private UUID entityId;
    private UUID objectId;
    private UUID requestId;

    @Builder.Default
    private List<String> formats = new ArrayList<>();
}
