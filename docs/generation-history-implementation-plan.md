# Generation History Implementation Plan

Дата: 2026-06-17

## Цель

Добавить отдельный read API для просмотра технической истории генерации документа,
не смешивая его с пользовательской витриной готовых документов.

`DocumentController#listDocuments` должен оставаться витриной результатов и
возвращать только готовые generated files. История генерации должна быть
доступна отдельным endpoint-ом.

## Предлагаемый контракт

Вариант endpoint:

```http
GET /v1/doc/{documentId}/generation-history
```

Альтернатива для admin/diagnostics namespace:

```http
GET /internal/doc/{documentId}/generation-history
```

Рекомендуемый response shape:

```json
{
  "documentId": "uuid",
  "files": [
    {
      "fileId": "uuid",
      "format": "DOCX",
      "status": "DONE",
      "job": {
        "jobId": "uuid",
        "status": "DONE",
        "attemptCount": 2,
        "nextRetryAt": null
      },
      "attempts": [
        {
          "attemptId": "uuid",
          "attemptNo": 1,
          "status": "ERROR",
          "errorCode": "core_client.request_failed",
          "errorMessage": "...",
          "startedAt": "...",
          "finishedAt": "...",
          "workerId": "uuid"
        },
        {
          "attemptId": "uuid",
          "attemptNo": 2,
          "status": "DONE",
          "artifactS3Key": "...",
          "artifactChecksum": "...",
          "artifactSizeBytes": 12345,
          "startedAt": "...",
          "finishedAt": "...",
          "workerId": "uuid"
        }
      ]
    }
  ]
}
```

## Архитектурный подход

Ввести отдельный read-side pipeline:

- `GenerationHistoryQueryRepository`;
- `GenerationHistoryQueryService`;
- `DocumentGenerationHistoryUseCase`;
- `DocumentGenerationHistoryWebAdapter`;
- DTO в api-модуле для history response.

Repository должен читать агрегированную историю из:

- `t_generated_document`;
- `t_generated_file`;
- `t_generation_job`;
- `t_generation_job_attempt`.

Основной `DocumentRs` не расширять попытками и техническими полями, чтобы не
перегружать пользовательский list/get контракт.

## Правила выдачи

- История должна быть доступна для `PENDING`, `PROCESSING`, `DONE`, `ERROR`
  документов.
- Attempts сортируются по `attemptNo ASC`.
- Files сортируются так же, как в документном read API: по `createdAt ASC, id ASC`.
- Если job еще не была claimed и attempts отсутствуют, возвращается пустой
  массив `attempts`.
- Если generation упала до создания attempt, history должна показывать состояние
  job/file и отсутствие attempts.

## Тесты

Минимальное покрытие:

- документ без attempts возвращает пустую history attempts;
- retry-сценарий возвращает несколько attempts в правильном порядке;
- финальная ошибка содержит `errorCode` и `errorMessage`;
- successful attempt содержит artifact metadata;
- неизвестный `documentId` возвращает `document.not_found`.

## Открытые решения

- Будет ли endpoint публичным или internal/admin diagnostics.
- Нужно ли показывать `workerName`, если сейчас он является log-only
  представлением runtime worker.
- Нужна ли пагинация attempts при очень длинной истории после будущего изменения
  retry policy.
