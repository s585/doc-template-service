package ru.sberbank.sbercrm.saas.doctemplate.application.integration.client.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import feign.codec.EncodeException;
import feign.form.multipart.AbstractWriter;
import feign.form.multipart.Output;
import org.springframework.http.MediaType;
import ru.sberbank.sbercrm.saas.doctemplate.application.integration.client.FileRq;

public class FileRqWriter extends AbstractWriter {
    private final ObjectMapper objectMapper;

    public FileRqWriter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean isApplicable(Object object) {
        return object instanceof FileRq;
    }

    @Override
    protected void write(Output output, String key, Object object) throws EncodeException {
        writeFileMetadata(output, key, null, MediaType.APPLICATION_JSON_VALUE);
        output.write(toJson((FileRq) object));
    }

    private byte[] toJson(FileRq fileRq) {
        try {
            return objectMapper.writeValueAsBytes(fileRq);
        } catch (JsonProcessingException ex) {
            throw new EncodeException("Failed to serialize multipart FileRq part", ex);
        }
    }
}
