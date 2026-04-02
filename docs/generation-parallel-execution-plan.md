# План перевода generation execution на параллельную модель

Дата: 2026-04-02

## Зачем меняем текущую модель

Текущая реализация generation использует периодический polling c `fixedDelay = 5000 ms` и последовательно обрабатывает весь захваченный batch внутри одного потока.

Это создает два ограничения:

- новая job может ждать до 5 секунд даже при пустой системе;
- внутри одного инстанса нет параллелизма, поэтому долгие генерации выстраиваются в очередь.

## Цель изменения

Перевести generation execution на модель:

- частый dispatch с низкой latency;
- ограниченный параллелизм внутри одного инстанса;
- безопасный claim job из БД без двойной обработки;
- отдельная orchestration-роль для dispatch и отдельная execution-роль для обработки одной job;
- покрытие unit и integration тестами.

## Целевая архитектура

### 1. Scheduler

`GenerationJobScheduler` больше не должен выполнять batch сам.

Он должен:

- тикать часто, например раз в `200-500 ms`;
- проверять, что generation включена;
- делегировать в `GenerationJobDispatchUseCase`.

### 2. Dispatch use case

Новый `GenerationJobDispatchUseCase` отвечает за orchestration:

- узнает доступную емкость worker pool;
- если свободных слотов нет, завершает проход;
- запрашивает у репозитория только столько job, сколько реально можно начать прямо сейчас;
- отправляет каждую job в `TaskExecutor`.

Dispatch не должен выполнять генерацию сам.

### 3. Single-job execution use case

`GenerationJobExecutionUseCase` должен обрабатывать одну job:

- перевод файла в `PROCESSING`;
- скачивание шаблона;
- генерация;
- загрузка результата;
- перевод файла и job в `DONE` или `ERROR`.

Batch API в execution use case больше не нужен.

### 4. Worker pool

Нужен отдельный `ThreadPoolTaskExecutor` для generation:

- фиксируем контролируемый уровень параллелизма;
- не держим длинную in-memory очередь;
- подбираем количество job под реальные свободные слоты.

Рекомендуемая стартовая конфигурация:

- `pool-size = 4`
- `queue-capacity = 0`

## Изменения в конфигурации

В `TemplateProperties.Generation` и `application.yml` вводим:

- `dispatch-fixed-delay-ms`
- `worker-pool-size`

Текущий `batch-size` становится не основным регулятором и должен быть заменен логикой claim по количеству доступных слотов.

## Изменения в persistence/repository

`GenerationJobRepository` должен уметь:

- `claimNextJobs(UUID workerId, int limit)` — атомарно захватывать не более `limit` job;
- при claim переводить job в `PROCESSING` и проставлять `workerId`;
- возвращать именно список захваченных job.

На текущем этапе достаточно сохранить существующую модель без retry/requeue timed out jobs, но API репозитория нужно выровнять под новый dispatch.

## Изменения в сервисном слое

`GenerationJobService` должен отдать orchestration-friendly API:

- `claimNextJobs(UUID workerId, int limit)`
- `markCompleted(...)`
- `markFailed(...)`

## Изменения в тестах

Нужно покрыть:

### Unit

- dispatch use case:
  - не делает claim, если свободных слотов нет;
  - делает claim по capacity;
  - отправляет каждую job в executor;
- execution use case:
  - успешная обработка одной job;
  - ошибочная обработка одной job.

### Integration

- существующий integration happy path на generation должен остаться зеленым;
- по возможности проверить, что scheduler/dispatch не ломают текущий flow.

## Порядок реализации

1. Добавить артефактный план.
2. Ввести `GenerationJobDispatchUseCase`.
3. Переделать `GenerationJobExecutionUseCase` на single-job модель.
4. Добавить `ThreadPoolTaskExecutor` и обвязку для оценки доступной capacity.
5. Обновить scheduler.
6. Обновить service/repository API под `claimNextJobs`.
7. Добавить unit-тесты dispatch.
8. Актуализировать execution tests.
9. Прогнать полный `mvn test`.

## Ограничения первого шага

На первом шаге сознательно не включаем:

- heartbeat;
- возврат зависших `PROCESSING` job;
- retry policy;
- `generation_job_attempt` orchestration;
- broker.

Это следующий слой развития после стабилизации параллельной модели внутри приложения.
