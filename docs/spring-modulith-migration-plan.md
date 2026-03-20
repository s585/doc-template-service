# Spring Modulith Migration Plan

## Цель

Подготовить проект к дальнейшему выделению частей системы в отдельные микросервисы через явные модульные границы внутри монолита.

`Spring Modulith` используется не как самоцель, а как инструмент для:
- фиксации модульных границ;
- контроля допустимых зависимостей между контекстами;
- поэтапной декомпозиции без немедленного перехода к распределенной архитектуре.

## Целевые модули

### `ru.sberbank.sbercrm.doctemplate.template`

Ответственность:
- управление шаблонами;
- import / update / delete / list;
- mappings;
- display conditions;
- обработка файлов шаблонов в части чтения структуры шаблона.

Ожидаемые подпакеты:
- `controller`
- `adapter`
- `usecase`
- `service`
- `repository`
- `model`
- `processor`
- `converter`
- `config`

### `ru.sberbank.sbercrm.doctemplate.generation`

Ответственность:
- генерация документов по шаблонам;
- generated document / generated file;
- generation job / attempt;
- orchestration генерации.

Ожидаемые подпакеты:
- `controller`
- `adapter`
- `usecase`
- `service`
- `repository`
- `model`
- `processor`
- `converter`

### `ru.sberbank.sbercrm.doctemplate.integration.filestorage`

Ответственность:
- интеграция с внешним file storage;
- Feign client;
- адаптеры и модели интеграции.

Ожидаемые подпакеты:
- `client`
- `adapter`
- `model`
- `config`

### `ru.sberbank.sbercrm.doctemplate.shared`

Ответственность:
- только действительно общие технические элементы;
- исключения;
- константы;
- JSON / helper;
- paging abstractions;
- jOOQ helper classes.

Ожидаемые подпакеты:
- `exception`
- `constant`
- `helper`
- `model`
- `jooq`

## Правила зависимостей

Разрешенные зависимости:
- `template -> shared`
- `template -> integration.filestorage`
- `generation -> shared`
- `generation -> integration.filestorage`
- `generation -> template` только если генерация использует публичный API модуля шаблонов

Запрещенные зависимости:
- `template -> generation`
- `integration.filestorage -> template`
- `integration.filestorage -> generation`

## Правила проектирования модулей

1. Взаимодействие между модулями должно идти через публичные application-level контракты.
2. Внутренние репозитории, внутренние модели и внутренние адаптеры модуля не должны использоваться соседними модулями напрямую.
3. `shared` не должен становиться свалкой доменной логики.
4. Если сущность используется только в одном контексте, она должна оставаться в этом контексте.
5. `expression`, `rule`, `source` должны оставаться внутри `template`, пока не появится подтвержденное переиспользование вне этого контекста.

## API-модуль

API-пакеты должны зеркалировать бизнес-контексты:

- `ru.sberbank.sbercrm.doctemplate.template.controller`
- `ru.sberbank.sbercrm.doctemplate.template.dto`
- `ru.sberbank.sbercrm.doctemplate.template.dto.expression`
- `ru.sberbank.sbercrm.doctemplate.template.dto.rule`
- `ru.sberbank.sbercrm.doctemplate.template.dto.source`
- `ru.sberbank.sbercrm.doctemplate.shared.dto`
- `ru.sberbank.sbercrm.doctemplate.shared.contract`

При появлении generation API:
- `ru.sberbank.sbercrm.doctemplate.generation.controller`
- `ru.sberbank.sbercrm.doctemplate.generation.dto`

## Предварительная карта текущего кода

Уже близко к целевому состоянию:
- `...template.*`
- `...shared.*`

Потребует переноса при отдельной задаче:
- `...template.adapter.filestorage.*` -> `...integration.filestorage.*`
- generation-related пакеты после реализации generation use cases -> `...generation.*`

## План отдельной задачи

### Этап 1. Зафиксировать модульные границы

- добавить зависимость `Spring Modulith`;
- описать application modules через `package-info.java`;
- задать `allowedDependencies`.

### Этап 2. Довести package structure до модульной карты

- вынести filestorage интеграцию в `integration.filestorage`;
- проверить, что `shared` содержит только технические общие элементы;
- убрать прямые зависимости между внутренностями модулей.

### Этап 3. Ограничить публичную поверхность модулей

- определить публичные фасады / use cases модулей;
- исключить прямое использование внутренних `repository` / `model` соседними модулями;
- зафиксировать boundary rules в тестах.

### Этап 4. Добавить модульные тесты

- tests на допустимость зависимостей;
- tests на отсутствие циклов между модулями;
- tests на публичные API модулей.

### Этап 5. Подготовить точки будущего выделения в сервисы

- определить, какие модули могут быть выделены первыми;
- оценить текущие синхронные зависимости;
- определить места, где в будущем стоит заменить прямой вызов на событие или внешний контракт.

## Не делать в рамках текущей задачи

- не перестраивать весь проект под modulith немедленно;
- не выносить все подряд в `shared`;
- не выделять `generation` до завершения ключевых generation use cases;
- не смешивать доменные модули и инфраструктурные интеграции.

## Критерий готовности отдельной задачи

Отдельную задачу по `Spring Modulith` можно считать выполненной, если:
- модули явно описаны;
- модульные зависимости проверяются автоматически;
- `template`, `generation`, `integration.filestorage`, `shared` структурно разведены;
- нарушение границ модулей приводит к падению тестов или проверок сборки.
