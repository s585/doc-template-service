package ru.sberbank.sbercrm.saas.doctemplate.shared.dto;

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
public class CheckDataByFilterRqDto {
    public List<FilterDto> filter;
    public Map<String, Object> data;
}
