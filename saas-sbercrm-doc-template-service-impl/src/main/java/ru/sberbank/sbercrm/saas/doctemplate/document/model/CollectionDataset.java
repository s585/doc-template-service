package ru.sberbank.sbercrm.saas.doctemplate.document.model;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Данные для одного повторяемого collection-блока шаблона.
 *
 * <p>{@code keys} содержит placeholder-ы этого блока, а {@code rows} хранит значения
 * для каждого повторения блока. Процессоры DOCX/XLSX используют эти данные, чтобы
 * размножить строку или область шаблона по элементам коллекции.
 */
@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class CollectionDataset {
    private CollectionQueryKey queryKey;
    @Builder.Default
    private Set<String> keys = new LinkedHashSet<>();
    @Builder.Default
    private List<Map<String, String>> rows = new ArrayList<>();
}
