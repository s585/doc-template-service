package ru.sberbank.sbercrm.saas.doctemplate.template.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.multipart.MultipartFile;
import ru.sberbank.sbercrm.saas.doctemplate.shared.dto.CommonRqDto;
import ru.sberbank.sbercrm.saas.doctemplate.shared.dto.CommonRsDto;
import ru.sberbank.sbercrm.saas.doctemplate.template.dto.TemplateCreationRq;
import ru.sberbank.sbercrm.saas.doctemplate.template.dto.TemplateRs;
import ru.sberbank.sbercrm.saas.doctemplate.template.dto.TemplateUpdateRq;

import java.util.UUID;

@Tag(
    name = "Шаблоны печатных форм",
    description = "Контракты API для управления шаблонами печатных форм"
)
public interface TemplateController {
    @Operation(summary = "Импортировать шаблон")
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Шаблон успешно импортирован",
            content = @Content(schema = @Schema(implementation = TemplateRs.class))
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Некорректный формат файла или дублирующийся code"
        )
    })
    @PostMapping(
        value = "/v1/doc/template/import",
        consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    TemplateRs importTemplate(
        @Valid @RequestPart("request") TemplateCreationRq request,
        @RequestPart("file") MultipartFile file
    );

    @Operation(summary = "Обновить шаблон")
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Шаблон успешно обновлен",
            content = @Content(schema = @Schema(implementation = TemplateRs.class))
        ),
        @ApiResponse(responseCode = "400", description = "Некорректное тело запроса"),
        @ApiResponse(responseCode = "404", description = "Шаблон не найден")
    })
    @PutMapping(
        value = "/v1/doc/template/{templateId}",
        consumes = MediaType.APPLICATION_JSON_VALUE,
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    TemplateRs updateTemplate(
        @PathVariable("templateId") UUID templateId,
        @Valid @org.springframework.web.bind.annotation.RequestBody TemplateUpdateRq request
    );

    @Operation(summary = "Получить шаблон по идентификатору")
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Шаблон найден",
            content = @Content(schema = @Schema(implementation = TemplateRs.class))
        ),
        @ApiResponse(responseCode = "404", description = "Шаблон не найден")
    })
    @GetMapping(
        value = "/v1/doc/template/{templateId}",
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    TemplateRs getTemplate(@PathVariable("templateId") UUID templateId);

    @Operation(summary = "Удалить шаблон")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Шаблон успешно удален"),
        @ApiResponse(responseCode = "404", description = "Шаблон не найден")
    })
    @DeleteMapping("/v1/doc/template/{templateId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void deleteTemplate(@PathVariable("templateId") UUID templateId);

    @Operation(summary = "Получить список шаблонов")
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Список шаблонов",
            content = @Content(schema = @Schema(implementation = CommonRsDto.class))
        ),
        @ApiResponse(responseCode = "400", description = "Некорректный запрос списка")
    })
    @PostMapping(
        value = "/v1/doc/template/list",
        consumes = MediaType.APPLICATION_JSON_VALUE,
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    CommonRsDto listTemplates(
        @Valid
        @RequestBody(description = "Параметры фильтрации, сортировки и пагинации")
        @org.springframework.web.bind.annotation.RequestBody CommonRqDto request
    );

    @Operation(summary = "Получить список доступных шаблонов для объекта")
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Список доступных шаблонов",
            content = @Content(schema = @Schema(implementation = CommonRsDto.class))
        ),
        @ApiResponse(responseCode = "404", description = "Сущность или объект не найдены")
    })
    @PostMapping(
        value = "/v1/doc/{entityId}/{objectId}/template/list",
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    CommonRsDto listAvailableTemplates(
        @PathVariable UUID entityId,
        @PathVariable UUID objectId
    );
}
