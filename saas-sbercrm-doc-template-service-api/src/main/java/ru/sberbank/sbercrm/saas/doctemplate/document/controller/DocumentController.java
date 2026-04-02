package ru.sberbank.sbercrm.saas.doctemplate.document.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import ru.sberbank.sbercrm.saas.doctemplate.shared.dto.CommonRqDto;
import ru.sberbank.sbercrm.saas.doctemplate.shared.dto.CommonRsDto;
import ru.sberbank.sbercrm.saas.doctemplate.document.dto.DocumentCreationRq;
import ru.sberbank.sbercrm.saas.doctemplate.document.dto.DocumentRs;

import java.util.UUID;

@Tag(
    name = "Генерация печатных форм",
    description = "Контракты API для запуска и отслеживания генерации документов"
)
public interface DocumentController {

    @Operation(summary = "Запустить генерацию документа")
    @PostMapping(
        value = "/v1/doc/generated",
        consumes = MediaType.APPLICATION_JSON_VALUE,
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    @ResponseStatus(HttpStatus.ACCEPTED)
    DocumentRs createDocument(@Valid @RequestBody DocumentCreationRq request);

    @Operation(summary = "Получить статус генерации документа")
    @GetMapping(
        value = "/v1/doc/{documentId}",
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    DocumentRs getDocument(@PathVariable("documentId") UUID documentId);

    @Operation(summary = "Получить список документов по объекту с пагинацией")
    @PostMapping(
        value = "/v1/doc/{entityId}/{objectId}/list",
        consumes = MediaType.APPLICATION_JSON_VALUE,
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    CommonRsDto listDocuments(
        @PathVariable("entityId") UUID entityId,
        @PathVariable("objectId") UUID objectId,
        @Valid @RequestBody CommonRqDto request
    );
}
