package ru.sberbank.sbercrm.doctemplate.template.adapter.filestorage;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class FolderRq {
    private String path;
    private String source;
    private Map<String, List<String>> tags;
}
