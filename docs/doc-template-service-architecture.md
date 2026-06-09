# Document Template Service Architecture

## Статус документа

- Этот документ является рабочим архитектурным описанием сервиса.
- Для всех последующих изменений в generation flow он должен рассматриваться как текущий source of truth.
- Если реализация начинает расходиться с этим документом, сначала должен обновляться документ, затем код.
- После внесения любых изменений, влияющих на поведение сервиса, архитектуру,
  модель данных, обработку задач, статусы, интеграции или контракт взаимодействия
  между внутренними слоями, соответствующие изменения должны быть обязательно
  зафиксированы в этом документе в рамках той же задачи.

## Назначение сервиса

Сервис отвечает за:

- хранение и чтение шаблонов документов;
- импорт шаблонов и извлечение переменных;
- создание документов на генерацию;
- асинхронную генерацию файлов по шаблонам;
- хранение результатов генерации и статусов файлов.

Сервис не является источником бизнес-данных объекта. Он использует `entityId`
и `objectId` как внешние идентификаторы доменного объекта, для которого
генерируется документ.

## Границы модулей

### `saas-sbercrm-doc-template-service-api`

Содержит внешний API-контракт:

- контроллеры;
- request/response DTO;
- публичные enum/контракты, необходимые клиенту.

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

- web adapter / controller implementation;
- use case;
- service;
- repository;
- интеграции с внешними системами;
- процессинг шаблонов;
- scheduler / worker execution.

## Доменные сущности и их смысл

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

`Document` не имеет собственного статуса. Источник правды о ходе генерации находится на уровне файлов.

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

Дополнительные runtime-поля:

- `attempt_count`
- `next_retry_at`

### GenerationJobAttempt

Журнал попыток выполнения `GenerationJob`.

На текущий момент `generation_job_attempt` уже встроен в runtime flow:

- при старте обработки job создаётся attempt со статусом `PROCESSING`;
- после успешной загрузки артефакта в file-storage attempt может быть переведена в `UPLOADED`
  в отдельной транзакции;
- при успешном завершении flow attempt переводится в `DONE`;
- при неуспешном завершении attempt переводится в `ERROR`;
- при timeout recovery активная attempt переводится в `TIMEOUT`.

Поля артефакта в `generation_job_attempt`:

- `artifact_s3_key`
- `artifact_checksum`
- `artifact_size_bytes`

Они используются для re-use артефакта на retry-попытках без повторной генерации.
Для retry reuse наличие checksum/size считается обязательным.

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

Конвертация `jooq.Record -> model` должна быть вынесена в отдельные converter/mapper-классы по аналогии с доменом `template`.

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
- системная, инфраструктурная или конфигурационная ошибка должна выражаться через `SystemCrmException`;
- все исключения сервиса должны наследоваться от `AbstractCrmException`.

Это правило распространяется в том числе на:

- use case;
- service;
- repository helper;
- processor resolver;
- state machine;
- внутренние utility-компоненты, влияющие на runtime поведение сервиса.

Дополнительное правило для наследников `AbstractCrmException`:

- конструкторы исключений должны передавать `cause` и `params` в базовый класс без искажений;
- отсутствие параметров должно сохраняться как пустой массив параметров, а не как вложенный массив;
- отсутствие `cause` не должно приводить к тому, что `Throwable` попадает в `params`.

### 8. Время в сервисе централизовано через `Clock`

В `impl`-модуле создаётся единый Spring bean:

- `Clock.systemUTC()`

Все runtime-временные метки, которые сервис вычисляет сам, должны ставиться через:

- `OffsetDateTime.now(clock)`

Это правило распространяется в том числе на:

- расчёт `nextRetryAt`;
- lease/timeout метки generation job;
- `updatedAt`, когда время выставляется из Java-кода;
- `finishedAt` для `generation_job_attempt`.

Использование `OffsetDateTime.now()` без `clock` и `DSL.currentOffsetDateTime()`
для прикладных timestamp-полей в сервисном коде не допускается.

## File storage

Сервис использует gateway над file storage:

- production-вариант через внешний клиент;
- локальный stub-вариант для разработки и интеграционных тестов.

`FileStorageGatewayStub` нужен для работы с локальными файлами без внешнего file storage.

Stub поддерживает:

- `upload`;
- `download` c возвратом `byte[]`;
- `findAllByFilter`;
- `delete`.

При `saas.doc-template.file-storage.stub-enabled=true` поднимается
dev endpoint `GET /internal/dev/file-storage/file?key=...`, который возвращает
абсолютный локальный путь файла в stub-storage. Endpoint нужен только для
локальной отладки штатного пути генерации и не активен при production gateway.
Локальная разработка включается профилем `local`: в `application-local.yml`
вместе задаются `stub-enabled=true`, `folder=templates` и
`stub-root-path=${user.dir}`. В этом режиме
импортированные шаблоны пишутся в
`templates/{entityId}/{randomUuid}_{templateFileName}`, а сгенерированные
документы пишутся в
`documents/{entityId}/{objectId}/{documentId}/{randomUuid}_{generatedFileName}`.

## Business object integration

Сервис использует единый gateway над business-object сервисом:

- `BusinessObjectGateway` (production runtime);
- вызов выполняется через `Feign`-клиент;
- для интеграционных тестов используется `WireMock` (а не файловый stub).

Для постраничной выборки связанных business objects используется отдельный
`BusinessObjectPageIterator`.

Архитектурное правило для `REFERENCE`-lookup:

- gateway отвечает за загрузку одной страницы и маппинг ошибок интеграции;
- итерация по страницам вынесена в отдельный iterator-компонент;
- reference-коллекции обрабатываются page-by-page без накопления всех raw
  business objects в памяти;
- в runtime-контекст попадают только уже спроецированные значения строк
  коллекции.

Классификация ошибок интеграции с core:

- `404` -> `generation.business_object_not_found` (non-retriable);
- транспортные и временные ошибки core (`RetryableException`, HTTP `5xx`, `429`, `408`)
  -> `core_client.request_failed` (retriable для generation job);
- прочие ошибки core -> `system.unexpected`.

Для `core-client` включён client-specific inline retry (`Feign Retryer`) с настройками
из приложения:

- `saas.doc-template.integration.core-client.retry.period-ms`
- `saas.doc-template.integration.core-client.retry.max-period-ms`
- `saas.doc-template.integration.core-client.retry.max-attempts`

По умолчанию: `100ms / 500ms / 3 attempts`.

Итоговый retry-механизм двухуровневый:

- сначала короткие inline повторы на уровне Feign клиента core;
- если не помогло, применяется job-level retry policy generation.

Контракт `DIRECT`-source path:

- root всегда `source`;
- поддерживается только `source.<dot-path>` (как минимум один field-сегмент после `source.`);
- `$`/`$.` не являются валидным root.

Если business-object не найден, используется business error:

- `generation.business_object_not_found`.

## Внешний API документов

Поддерживаются операции:

- создание документа на генерацию;
- получение документа по `documentId`;
- получение списка документов по `entityId + objectId` с пагинацией.

Список документов использует общий пагинируемый контракт сервиса.

## Поток обработки generation сейчас

### Создание документа

При создании документа сервис:

1. проверяет существование шаблона;
2. создаёт запись в `generated_document`;
3. создаёт по файлу на формат в `generated_file` со статусом `PENDING`;
4. создаёт по job на формат в `generation_job` со статусом `QUEUED`.

### Dispatch

Scheduler работает часто и не выполняет job сам.

Сам bean `GenerationJobScheduler` создаётся только при
`saas.doc-template.generation.scheduler-enabled=true`.

Он вызывает dispatch use case, который:

1. делает recovery просроченных `PROCESSING` job;
2. вычисляет свободные worker slots;
3. claim-ит из БД только нужное число job;
4. отправляет каждую job в `ThreadPoolTaskExecutor`.

Для worker execution используется отдельная runtime identity:

- `workerId` хранится в БД в `generation_job.locked_by` и `generation_job_attempt.worker_id`;
- `workerId` является стабильным `UUID` конкретного runtime-инстанса сервиса на протяжении его жизни;
- для расследования инцидентов используется не только `workerId`, но и человекочитаемый `workerName` в логах;
- `workerName` имеет вид `applicationName@host:pid`, а в execution-логах дополняется именем потока.

В claim попадают только job, для которых:

- `status = QUEUED`;
- `next_retry_at is null` или `next_retry_at <= now()`;
- lease-блокировка не активна.

### Execution

Один execution use case обрабатывает одну job:

1. через транзакционный transition service атомарно создаёт `generation_job_attempt`
   и переводит связанный `generated_file` в `PROCESSING`;
2. нормализует ошибку через `GenerationErrorClassifier`, который поднимается по
   cause chain до `AbstractCrmException` и принимает решение по `errorCode`;
3. строит явный `GenerationRetryDecision` c действием `RETRY_NOW`, `RETRY_LATER` или `FAIL_FINAL`;
4. при ошибке до успешного создания attempt сразу передаёт retry/fail decision в transition service;
5. при `RETRY_NOW/RETRY_LATER` возвращает job в `QUEUED` с `next_retry_at`;
6. при `FAIL_FINAL` завершает job в `ERROR`;
7. при успехе завершает `generated_file + generation_job + generation_job_attempt` через транзакционный transition service.

Подготовка generation context вынесена в отдельный компонент
`GenerationContextAssembler` и выполняется в явном пайплайне:

- resolve `source`;
- evaluate `expression`;
- привести к `String` для шаблонного процессора.

Для generation mappings используется явный planning step:

- `GenerationMappingPlanner` делит mappings на scalar-ветку, file-name-ветку и
  collection groups;
- grouping коллекций строится по `CollectionQueryKey`, который описывает
  конкретный lookup reference-данных;
- assembler не содержит source-specific логики reference lookup и работает через
  resolver-слой.

`GenerationTemplateContext` содержит только runtime-данные:

- `scalarValues`;
- `collections`;
- `generatedFileName`.

Collection runtime-модель строится вокруг `CollectionDataset`:

- один dataset соответствует одному reference lookup;
- dataset содержит `queryKey`, набор `keys` и `rows`;
- `rows` представлены как `List<Map<String, String>>`, то есть в row-based
  форме, пригодной для прямого рендера в `DOCX` и `XLSX`.

Expression-обработка применяется как к scalar values, так и к значениям внутри
collection rows.

## State machine generation job

Для `generation_job` используется lightweight state machine в коде.

Это не внешняя библиотека и не generic engine. Это локальная модель допустимых переходов.

### События

- `CLAIM`
- `TIMEOUT`
- `RETRY`
- `COMPLETE`
- `FAIL`

### Разрешённые переходы

- `QUEUED --CLAIM--> PROCESSING`
- `PROCESSING --TIMEOUT--> QUEUED`
- `PROCESSING --RETRY--> QUEUED`
- `PROCESSING --COMPLETE--> DONE`
- `PROCESSING --FAIL--> ERROR`

Любой другой переход считается ошибкой реализации и должен завершаться исключением.

## Согласованность данных

Полная атомарность всего pipeline невозможна, потому что в процессе участвует внешний file storage.

Поэтому используется следующее правило:

- внешние side effects выполняются вне DB-транзакции;
- финальная фиксация статусов в БД должна выполняться короткой транзакцией;
- обновление `generated_file` и `generation_job` должно происходить согласованно в одном transaction boundary.

Для этого используется `GenerationJobTransitionService`.

Сейчас через него проходят:

- старт attempt и перевод файла в `PROCESSING`;
- обработка ошибки до создания attempt;
- успешное завершение генерации;
- завершение с ошибкой;
- планирование retry;
- возврат просроченной job обратно в очередь;
- синхронное обновление `generation_job_attempt` в том же transition boundary.

## Подстановка значений в шаблон

`DOCX` и `XLSX` процессоры используют однофазную подстановку по найденным
placeholder-ам шаблона, а не последовательный `replace(...)` по `Map`.

Следствия:

- результат не зависит от порядка обхода `values`;
- вложенные/каскадные подстановки не поддерживаются и не являются контрактом;
- отсутствие значения оставляет исходный placeholder без изменений.

### Повторяющиеся блоки

Повторяемые блоки рендерятся не по явной пользовательской разметке, а по
структуре документа.

Для `DOCX`:

- строка таблицы является repeat-unit для табличных коллекций;
- пункт нумерованного или маркированного списка является repeat-unit для
  списочных коллекций.

Для `XLSX`:

- repeat-unit — одна строка листа.

Правила рендера repeat-блоков:

- в одном repeat-блоке допускается только одна collection dataset;
- scalar placeholders могут использоваться внутри repeat-блока и повторяются
  в каждой его копии;
- пустая коллекция удаляет шаблонный block/row;
- неоднозначный dataset или частично незаполненный repeat-блок считаются
  ошибкой generation.

Корректность `COLLECTION` mappings проверяется до вызова процессора:

- все declared collection keys должны войти ровно в один dataset;
- один collection key не может принадлежать нескольким dataset-ам;
- ошибки конфигурации должны завершать generation до этапа layout/rendering.

## Валидация template mappings

Семантическая валидация mappings вынесена в отдельный `TemplateMappingValidator`.

Текущие обязательные правила:

- `REFERENCE` разрешен только для `COLLECTION`;
- `COLLECTION` с заполненным source разрешен только для `REFERENCE`;
- зарезервированный `generated_file_name` разрешен только со `scope = FILE_NAME`;
- `generated_file_name` не может использовать `REFERENCE` source.

Внутренний контракт transition-слоя должен передаваться не россыпью аргументов, а через
явные parameter object:

- `GenerationTransitionContext` для общего контекста перехода;
- `GenerationJobPreAttemptContext` для переходов, выполняемых до создания attempt;
- `GeneratedFileResult` для результата успешной генерации.

## Recovery зависших job

Сервис считает job зависшей, если:

- `generation_job.status = PROCESSING`
- `locked_until < now()`

Recovery работает перед каждым dispatch cycle.

При recovery:

1. активная attempt переводится в `TIMEOUT`;
2. timeout нормализуется тем же `GenerationErrorClassifier`, что и обычная execution error;
3. по нему строится тот же `GenerationRetryDecision`, что и для runtime-ошибок worker;
4. если решение равно `RETRY_NOW/RETRY_LATER`, state machine валидирует переход
   `PROCESSING -> QUEUED`, `generated_file` переводится в `PENDING`, а
   `generation_job` получает новый `next_retry_at`;
5. если решение равно `FAIL_FINAL`, job и файл завершаются в `ERROR`.

Цель recovery:

- вернуть в работу job, брошенные упавшим worker;
- не оставлять публичный файл в вечном `PROCESSING`.

## Почему job может зависнуть

Типовые причины:

- падение инстанса приложения после claim;
- аварийное завершение JVM/контейнера;
- зависание или долгий timeout внешнего file storage;
- зависание процессинга шаблона;
- сбой после внешнего side effect и до финального обновления БД.

## Retry-модель

Retry-модель уже частично реализована и является обязательной частью generation flow.

### Цели retry

- повторять только временные ошибки;
- не зацикливать job на детерминированно плохом входе;
- хранить историю попыток;
- управлять backoff.

### Классы ошибок

#### Retriable

- временные сетевые ошибки;
- timeout внешнего вызова;
- временная недоступность file storage;
- инфраструктурные `5xx`.

#### Non-retriable

- битый или неподдерживаемый шаблон;
- неподдерживаемый формат;
- ошибка маппинга;
- отсутствие обязательных данных;
- детерминированная ошибка процессинга.

### Планируемая модель хранения retry

В `generation_job` используются агрегированные runtime-поля:

- `attempt_count`;
- `next_retry_at`;

Лимит попыток хранится в конфигурации сервиса.

В `generation_job_attempt` пишется история каждой попытки:

- номер попытки;
- время старта;
- время завершения;
- статус попытки;
- код и текст ошибки;
- при необходимости `worker_id`.

### Правила retry

При ошибке выполнения:

1. ошибка классифицируется;
2. если ошибка non-retriable, job завершается в `ERROR`;
3. если ошибка retriable и лимит попыток не исчерпан, job возвращается в `QUEUED`;
4. повторный запуск должен учитывать `next_retry_at`;
5. если лимит исчерпан, job завершается в `ERROR`.

Текущая реализация использует:

- `GenerationErrorClassifier`;
- `GenerationRetryPolicy`;
- `attempt_count` как количество уже завершённых попыток;
- `attempt_no` из `generation_job_attempt` как номер текущей попытки;
- `next_retry_at` как барьер допуска job в следующий claim.

`GenerationErrorClassifier` не должен принимать retry-решение по "сырым" Java-исключениям вроде `IOException`.
Решение о retriable/non-retriable должно приниматься по нормализованным
исключениям сервиса, наследующим `AbstractCrmException`, и их error code.

### Точки улучшения retry policy

Следующие улучшения считаются целевыми и могут реализовываться поэтапно:

1. Добавить jitter к backoff.
   При массовых временных сбоях повторный запуск не должен происходить строго в
   одну и ту же секунду. Допустим небольшой случайный разброс поверх базового
   backoff, чтобы избежать burst-нагрузки после восстановления внешней системы.

2. Поддержать policy по типу ошибки, а не только общий `maxAttempts`.
   Разные классы ошибок могут требовать разного лимита попыток и разного
   backoff. Например, storage timeout и generic system error не обязаны жить по
   одной и той же retry-схеме.

3. Улучшить observability exhausted retries.
   При финальном переходе в `ERROR` после исчерпания попыток должны быть хорошо видны:
   - номер последней попытки;
   - общее число попыток;
   - last error code;
   - `jobId`, `attemptId`, `workerId`.

4. Явно определить поведение side effects при retry.
   Если внешний side effect уже произошёл, а финальная фиксация в БД не успела
   завершиться, retry должен быть безопасным. Для этого нужна зафиксированная
   стратегия работы с уже загруженным артефактом:
   - детерминированный file key;
   - overwrite-safe upload;
   - или явная cleanup/reuse политика.
   
   Рекомендуемое направление реализации:
   - целевой `file key` должен вычисляться детерминированно на основе `documentId + format`;
   - повторная попытка должна писать в тот же key, а не создавать новый случайный артефакт;
   - upload в storage должен быть overwrite-safe;
   - следующим усилением модели считается `reuse existing artifact`, когда retry
     после неоднозначного сбоя может не загружать файл повторно, а догонять
     состояние БД по уже существующему артефакту.

5. Добавить метрики retry lifecycle.
   Минимально нужны:
   - количество стартовавших попыток;
   - количество запланированных retry;
   - количество timeout recovery;
   - количество финальных ошибок;
   - количество exhausted retries.

6. Поддержать формат-специфичные timeout/backoff при необходимости.
   При необходимости `docx` и `xlsx` могут получить разные значения processing
   timeout или retry backoff, если реальные профили выполнения окажутся
   разными.

7. Усилить идемпотентность retry на уровне результата генерации.
   Повторный запуск одной и той же job не должен создавать неоднозначность на уровне `generated_file` и артефакта в storage.

### Backoff

Рекомендуется экспоненциальный или ступенчатый backoff.

Базовый вариант:

- попытка 1: `10s`
- попытка 2: `30s`
- попытка 3: `2m`
- попытка 4: `10m`

### Важное ограничение

Recovery зависших `PROCESSING` job и бизнес-retry не должны развиваться как две разные независимые модели.

Текущая реализация уже следует этому правилу:

- timeout worker-а считается одной из причин неуспешной попытки;
- retry decision принимается в одном месте;
- `generation_job_attempt` хранит историю и timeout, и обычных execution failure.
- recovery timed out jobs обрабатывается по схеме `одна job = одна отдельная транзакция`,
  чтобы сбой на одной записи не откатывал весь batch.

## Что считать обязательным инвариантом сервиса

- один формат результата соответствует одному `generated_file` и одной `generation_job`;
- публичный статус находится у файла, не у документа;
- scheduler не выполняет job сам, а только диспетчеризует;
- worker обрабатывает одну job;
- финальные переходы статусов проходят через transition service;
- недопустимые переходы `generation_job` запрещаются state machine;
- просроченный `PROCESSING` не должен оставаться в системе бесконечно;
- любые изменения generation flow должны проверяться тестами на happy path и recovery path.

## Тестовое покрытие generation

Сейчас generation flow покрыт не только unit-тестами, но и интеграционными сценариями уровня документа.

Интеграционно подтверждены:

- happy path генерации `docx` с реальным сохранением результата и проверкой содержимого файла;
- retriable failure path: первая попытка завершается временной ошибкой, job
  возвращается в `QUEUED`, следующая попытка успешно завершает файл;
- non-retriable failure path: детерминированная ошибка завершает `generated_file` и `generation_job` в `ERROR` без повторного claim.

## Observability generation worker

Для generation flow обязательны lifecycle-логи с привязкой к worker identity.

Используемые уровни:

- `INFO`:
  - claim пачки job;
  - старт attempt;
  - успешное завершение job;
- `WARN`:
  - retriable failure с планированием retry;
  - recovery timed out job с возвратом в `QUEUED`;
- `ERROR`:
  - финальное завершение job в `ERROR`;
  - timeout с исчерпанным лимитом попыток.

Минимальный набор полей для operational correlation:

- `jobId`;
- `attemptId`;
- `attemptNo`;
- `documentId`;
- `templateId`;
- `format`;
- `workerId`;
- `workerName`.

### Как использовать `workerId` при расследовании инцидентов

`workerId` должен трактоваться как технический correlation identifier runtime-инстанса generation worker.

Что означает `workerId`:

- это стабильный `UUID` конкретного runtime-инстанса сервиса на протяжении его жизни;
- он записывается в `generation_job.locked_by` при claim job;
- он записывается в `generation_job_attempt.worker_id` для попытки, начатой этим worker;
- он не является `userId`;
- он не является бизнес-идентификатором инициатора генерации;
- он не является человекочитаемым идентификатором хоста или pod.

Что даёт `workerId` в расследовании:

- позволяет связать claim в `generation_job` и конкретную запись в `generation_job_attempt`;
- позволяет понять, что timeout, retry или completion относятся к одному и тому же runtime worker;
- позволяет связать состояние в БД с lifecycle-логами по тому же `workerId`.

Что не даёт `workerId` сам по себе:

- по одному только `workerId` нельзя понять, на каком именно хосте или pod работала job;
- по одному только `workerId` нельзя понять, какой пользователь инициировал генерацию;
- `workerId` не предназначен для внешнего API и не должен использоваться как публичный идентификатор.

Как расследовать инцидент:

1. найти проблемную `generation_job` или `generation_job_attempt`;
2. взять `jobId`, `attemptId`, `attemptNo`, `workerId`;
3. найти в логах события с тем же `workerId`;
4. использовать `workerName` из логов для выхода на конкретный `application@host:pid[:thread]`;
5. по связке `workerId + workerName + jobId + attemptId` восстановить полный lifecycle execution.

Операционное правило:

- для расследования `workerId` всегда используется вместе с `workerName` и идентификаторами `job/attempt`;
- `workerId` без логов считается недостаточным источником для forensic-анализа;
- `workerName` является человекочитаемым представлением runtime-исполнителя и должен присутствовать во всех lifecycle-логах generation flow.

### Защита от устаревших попыток

- финальные переходы `generation_job` выполняются через guarded update с ожидаемыми
  `status=PROCESSING` и `attempt_count`;
- stale worker не должен иметь возможность завершить или зафейлить job после того,
  как timeout recovery уже вернул её в очередь или новая попытка была claimed;
- `generated_file` и `generation_job_attempt` обновляются только после успешного guarded
  перехода `generation_job`, чтобы старый worker не затёр состояние новой попытки.

### Идемпотентность результата генерации

- generation flow на стороне сервиса формирует детерминированный folder path для generated
  artifacts: `.../generated/{entityId}/{objectId}/{documentId}`;
- имя generated файла определяется шаблоном и его mapping-ами, поэтому при поиске reuse
  используется связка `prefixKey + originalFileName`;
- этого недостаточно для полной идемпотентности в рабочем контуре, потому что текущий
  контракт file storage при повторном `upload` существующего файла не перезаписывает его,
  а создаёт новый файл с timestamp suffix;
- поэтому при `attemptNo > 1` execution flow сначала делает поиск уже загруженного
  generated artifact через `getWithFilter(prefixKey + originalFileName)`;
- если найден существующий файл, generation не выполняется повторно:
  сервис скачивает найденный файл, считает checksum и завершает job через reuse
  существующего `key`;
- если файл не найден, выполняется обычный `generate + upload`;
- если найдено несколько файлов, используется самый свежий артефакт по
  `updatedDate`, а ситуация логируется как неоднозначная;
- такой reuse-path снижает риск повторной генерации после partial failure
  `upload succeeded, DB commit failed`, но не устраняет already orphaned files,
  которые могли остаться от старых попыток.

### Ошибка до создания attempt

- если execution падает до успешного создания `generation_job_attempt`, job не должна
  оставаться в `PROCESSING` до lease-timeout;
- в этом случае execution flow обязан сразу принять retry/fail decision и передать его
  в `GenerationJobTransitionService` через отдельный `GenerationJobPreAttemptContext`;
- transition service должен в одной транзакционной границе согласованно обновить
  `generation_job` и `generated_file` без ожидания recovery.

## Следующие шаги реализации

После фиксации этого документа следующим этапом должны быть:

1. перевод timeout lease в конфигурируемый runtime timeout вместо фиксированного значения;
2. уточнение классификации retriable/non-retriable ошибок по реальным интеграциям;
3. опциональная публикация attempt/history в технический read API или admin diagnostics;
4. добавление `generation_job_attempt`-ориентированной observability и метрик;
5. реализация data resolver для `DirectValueSource` и `ReferenceValueSource`.
