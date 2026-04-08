# Document Template Service Architecture

## Статус документа

- Этот документ является рабочим описанием архитектуры generation flow в текущей ветке.
- Для ветки `codex/generation-api-contract` он должен рассматриваться как локальный source of truth.
- Если реализация generation flow начинает расходиться с этим документом, сначала должен обновляться документ, затем код.
- После внесения изменений, влияющих на поведение generation flow, архитектуру, модель данных, обработку задач,
  статусы или интеграции, соответствующие изменения должны фиксироваться в этом документе в рамках той же задачи.

## Назначение сервиса

Сервис отвечает за:

- хранение и чтение шаблонов документов;
- импорт шаблонов и извлечение переменных;
- создание документов на генерацию;
- асинхронную генерацию файлов по шаблонам;
- хранение результатов генерации и статусов файлов.

Сервис не является источником бизнес-данных объекта. Он использует `entityId` и `objectId` как внешние
идентификаторы доменного объекта, для которого создаётся документ.

## Границы модулей

### `saas-sbercrm-doc-template-service-api`

Содержит внешний API-контракт:

- контроллеры;
- request/response DTO;
- публичные enum и контракты.

### `saas-sbercrm-doc-template-service-db`

Содержит базовые Liquibase-скрипты схемы:

- `template`;
- `template_mapping`;
- `generated_document`;
- `generated_file`;
- `generation_job`;
- `generation_job_attempt`.

### `saas-sbercrm-doc-template-service-impl`

Содержит прикладную и доменную реализацию:

- controller implementation и web adapter;
- use case;
- service;
- repository;
- интеграции с file storage;
- процессинг шаблонов;
- scheduler и worker execution.

## Доменные сущности

### Template

Шаблон документа.

Содержит:

- метаданные шаблона;
- ссылку на файл шаблона в file storage;
- набор mapping-правил.

### Document

Публичный агрегат чтения.

Содержит:

- метаданные созданного документа;
- список связанных `GeneratedFile`.

У `Document` нет собственного статуса. Источник правды о ходе генерации находится на уровне файлов.

### GeneratedFile

Файл результата по конкретному формату.

Один `GeneratedFile` соответствует одному формату результата.

Статусы:

- `PENDING`
- `PROCESSING`
- `DONE`
- `ERROR`

### GenerationJob

Техническая сущность orchestration для асинхронной обработки генерации.

Одна `GenerationJob` соответствует одному формату.

Статусы:

- `QUEUED`
- `PROCESSING`
- `DONE`
- `ERROR`

## Ключевые архитектурные соглашения

### 1. Одна job = один формат

Если документ запрошен в нескольких форматах, создаются:

- один `generated_document`;
- несколько `generated_file`;
- несколько `generation_job`.

### 2. Репозитории по таблицам

Каждый jOOQ repository работает только со своей таблицей.

Примеры:

- `JooqGeneratedDocumentRepository` работает только с `t_generated_document`;
- `JooqGeneratedFileRepository` работает только с `t_generated_file`;
- `JooqGenerationJobRepository` работает только с `t_generation_job`.

### 3. Доступ к repository только через service

Use case не должен ходить в repository напрямую.

Это правило распространяется и на read-side:

- use case чтения ходят в query service;
- query service уже работает с query repository.

### 4. Агрегация на read-side отдельно от command-side

Для чтения агрегата документа используется отдельный query/read подход.

Для записи используются табличные сервисы и репозитории.

### 5. Конвертация jOOQ record в модель

Конвертация `jooq.Record -> model` вынесена в отдельные converter/mapper-классы по аналогии с доменом `template`.

### 6. Audit-поля обязательны в доменной модели

Для домена `document` поля:

- `createdBy`
- `updatedBy`

считаются обязательными и должны быть заполнены.

`userId` передаётся наравне с `tenantId`.

### 7. В сервисе не используются `IllegalStateException`

Runtime-код сервиса не должен выбрасывать `IllegalStateException` наружу.

Если возникает ошибка:

- бизнес-ошибка должна выражаться через `BusinessCrmException` или `NotFoundCrmException`;
- системная или инфраструктурная ошибка должна выражаться через `SystemCrmException`.

## File storage

Сервис использует gateway над file storage:

- production-вариант через внешний клиент;
- локальный stub-вариант для разработки и тестов.

`FileStorageGatewayStub` нужен для работы с локальными файлами без внешнего file storage.

Stub поддерживает:

- `upload`;
- `download` c возвратом `byte[]`;
- `delete`.

## Внешний API документов

Поддерживаются операции:

- создание документа на генерацию;
- получение документа по `documentId`;
- получение списка документов по `entityId + objectId` с пагинацией.

Список документов использует общий пагинируемый контракт сервиса.

## Поток обработки generation в текущей ветке

### Создание документа

При создании документа сервис:

1. проверяет существование шаблона;
2. создаёт запись в `generated_document`;
3. создаёт по файлу на формат в `generated_file` со статусом `PENDING`;
4. создаёт по job на формат в `generation_job` со статусом `QUEUED`.

### Dispatch

Scheduler не выполняет job сам.

Сам bean `GenerationJobScheduler` создаётся только при
`saas.doc-template.generation.enabled=true`.

Он вызывает dispatch use case, который:

1. вычисляет свободные worker slots;
2. claim-ит из БД только нужное число job;
3. отправляет каждую job в `ThreadPoolTaskExecutor`.

Для claim используется один runtime `workerId`, сгенерированный в `GenerationJobDispatchUseCaseImpl`.

### Выполнение job

`GenerationJobExecutionUseCase` обрабатывает одну job:

1. переводит соответствующий `generated_file` в `PROCESSING`;
2. читает шаблон из file storage;
3. собирает map значений из mapping-правил шаблона;
4. генерирует файл через `TemplateProcessingFacade`;
5. загружает результат в file storage;
6. обновляет `generated_file` в `DONE`;
7. обновляет `generation_job` в `DONE`.

При ошибке:

- `generated_file` переводится в `ERROR`;
- `generation_job` переводится в `ERROR`;
- код ошибки сейчас нормализуется в `system.unexpected`.

## Подстановка значений в шаблон

В текущей ветке generation flow умеет полноценно работать только с `ConstantValueSource`.

Если mapping:

- не содержит source, подставляется пустая строка;
- содержит неподдерживаемый source, выполнение завершается ошибкой.

То есть `DirectValueSource` и `ReferenceValueSource` в этой ветке ещё не реализованы как рабочий data resolver.

## Ограничения текущей реализации

В текущей ветке ещё отсутствуют:

- state machine для `generation_job`;
- retry policy;
- recovery зависших `PROCESSING` job;
- attempt-история в runtime flow;
- конфигурируемый lease timeout для `generation_job.locked_until`
  вместо хардкода `5 minutes` в `claim`;
- централизованный `Clock`;
- расширенная observability worker lifecycle;
- локальный debug runner для генерации документа.

Эти возможности считаются следующими слоями эволюции generation flow и реализуются в последующих ветках.
