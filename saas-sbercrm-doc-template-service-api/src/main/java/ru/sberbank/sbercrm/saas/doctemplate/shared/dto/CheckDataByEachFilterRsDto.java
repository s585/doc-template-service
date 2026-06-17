package ru.sberbank.sbercrm.saas.doctemplate.shared.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class CheckDataByEachFilterRsDto {
    public FilterDto filter;
    public Boolean result;
}
