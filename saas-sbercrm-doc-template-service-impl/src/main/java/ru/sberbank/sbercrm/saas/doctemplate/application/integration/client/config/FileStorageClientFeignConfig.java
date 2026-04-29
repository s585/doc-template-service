package ru.sberbank.sbercrm.saas.doctemplate.application.integration.client.config;

import feign.codec.Encoder;
import feign.form.ContentType;
import feign.form.MultipartFormContentProcessor;
import feign.form.spring.SpringFormEncoder;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.boot.autoconfigure.http.HttpMessageConverters;
import org.springframework.cloud.openfeign.support.SpringEncoder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import com.fasterxml.jackson.databind.ObjectMapper;

@Configuration
public class FileStorageClientFeignConfig {
    @Bean
    public Encoder fileStorageClientEncoder(
        ObjectFactory<HttpMessageConverters> messageConverters,
        ObjectMapper objectMapper
    ) {
        SpringFormEncoder encoder = new SpringFormEncoder(new SpringEncoder(messageConverters));
        MultipartFormContentProcessor processor = (MultipartFormContentProcessor) encoder.getContentProcessor(ContentType.MULTIPART);
        processor.addFirstWriter(new FileRqWriter(objectMapper));
        return encoder;
    }
}
