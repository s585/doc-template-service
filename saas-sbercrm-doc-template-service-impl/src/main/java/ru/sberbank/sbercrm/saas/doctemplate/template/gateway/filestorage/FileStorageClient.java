package ru.sberbank.sbercrm.saas.doctemplate.template.gateway.filestorage;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

@FeignClient(name = "file-storage-client")
public interface FileStorageClient {
    String TENANT_ID_HEADER = "X-Tenant-Id";
    String USER_ID_HEADER = "X-User-Id";

    @PostMapping(
        value = "/internal/v1/file/upload",
        consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    FileRs upload(
        @RequestHeader(value = "source", required = false) String source,
        @RequestPart("property") FileRq property,
        @RequestPart("file") MultipartFile file,
        @RequestHeader(TENANT_ID_HEADER) UUID tenantId,
        @RequestHeader(USER_ID_HEADER) UUID userId
    ) throws IOException;

    @DeleteMapping("/internal/v1/file")
    void deleteFile(
        @RequestHeader(value = "source", required = false) String source,
        @RequestParam("key") String key,
        @RequestHeader(TENANT_ID_HEADER) UUID tenantId,
        @RequestHeader(USER_ID_HEADER) UUID userId
    );
}
