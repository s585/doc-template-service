package ru.sberbank.sbercrm.saas.doctemplate.application.jooq;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.jooq.JSONB;
import org.springframework.stereotype.Component;
import ru.sberbank.sbercrm.saas.doctemplate.application.exception.CrmErrorCodes;
import ru.sberbank.sbercrm.saas.doctemplate.application.exception.model.SystemCrmException;

@Component
@RequiredArgsConstructor
public class JsonbHelper {
    private final ObjectMapper objectMapper;

    @Nullable
    public JSONB toJsonb(@Nullable Object value) {
        if (value == null) {
            return null;
        }

        try {
            return JSONB.valueOf(objectMapper.writeValueAsString(value));
        } catch (JsonProcessingException e) {
            throw new SystemCrmException(CrmErrorCodes.JSONB_SERIALIZATION_FAILED, CrmErrorCodes.JSONB_SERIALIZATION_FAILED, e);
        }
    }

    @Nullable
    public <T> T fromJsonb(@Nullable JSONB jsonb, Class<T> type) {
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
