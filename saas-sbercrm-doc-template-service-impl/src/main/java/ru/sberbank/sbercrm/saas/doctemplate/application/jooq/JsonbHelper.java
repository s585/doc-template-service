package ru.sberbank.sbercrm.saas.doctemplate.application.jooq;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.jooq.JSONB;
import org.springframework.stereotype.Component;
import ru.sberbank.sbercrm.saas.doctemplate.application.exception.CrmErrorCodes;
import ru.sberbank.sbercrm.saas.doctemplate.application.exception.model.SystemCrmException;

@Component
@RequiredArgsConstructor
public class JsonbHelper {
    private final ObjectMapper objectMapper;

    public JSONB toJsonb(Object value) {
        if (value == null) {
            return null;
        }

        try {
            return JSONB.valueOf(objectMapper.writeValueAsString(value));
        } catch (JsonProcessingException e) {
            throw new SystemCrmException(CrmErrorCodes.JSONB_SERIALIZATION_FAILED, CrmErrorCodes.JSONB_SERIALIZATION_FAILED, e);
        }
    }

    public <T> T fromJsonb(JSONB jsonb, Class<T> type) {
        if (jsonb == null) {
            return null;
        }

        try {
            return objectMapper.readValue(jsonb.data(), type);
        } catch (JsonProcessingException e) {
            throw new SystemCrmException(CrmErrorCodes.JSONB_DESERIALIZATION_FAILED, CrmErrorCodes.JSONB_DESERIALIZATION_FAILED, e);
        }
    }
}
