# MVP Doc Template API (2026-02-19)

## Context
Источник: страница "[Impl] Новые печатные формы (MVP)" (локальная копия).

Нужно реализовать backend-контракты и DTO для сервиса печатных форм (MVP) согласно описанию.

## Scope
### API эндпоинты
1. `POST /v1/doc/template/import`
   - multipart/form-data: `file`, `name`, `description`, `code`
   - Ответ: `TemplateRs`
2. `PUT /v1/doc/template/{templateId}`
   - Body: `TemplateRq`
   - Обновление только переданных полей
   - Ответ: `TemplateRs`
3. `DELETE /v1/doc/template/{templateId}`
   - Ответ: `204 No Content`
4. `POST /v1/doc/template/list`
   - Body: `commonRq`
   - Фильтрация: `is_active`, `entity_id`, `name`, `description`, `format`
   - Ответ: массив `TemplateRs`
5. `POST /v1/doc/{entityId}/{objectId}/template/list`
   - Список доступных шаблонов с учетом `displayCondition`

### DTO
- `Rule` (RuleDto)
- `TemplateMapping`
- `TemplateRq`
- `TemplateRs`
- `commonRq` (общий запрос списка/фильтра)

## Business rules
- `code` должен быть уникальным. При дубле -> 400.
- Файл должен быть DOCX или XLSX. Иначе -> 400.
- При импорте распарсить файл на маппинги по placeholder-паттерну из конфигурации.
- При обновлении изменять только поля, переданные в `TemplateRq`.
- При удалении: удалить файл в S3 и удалить данные из БД (template + mappings).

## Integrations
- `filestorage`:
  - создание папки: `POST /internal/v1/folder`
  - загрузка файла: `POST /internal/v1/file/upload`

## Acceptance criteria
- Контракты и DTO соответствуют описанию страницы.
- Эндпоинты соответствуют URL/методам.
- Негативные сценарии (400): дубль `code`, неверный формат файла.
- `list` фильтрует по `is_active`, `entity_id`, `name`, `description`, `format`.
- `DELETE` возвращает 204.

## Notes
- Использовать существующую БД `doctemplate` и таблицы `t_*`.
- Реализация в модуле `saas-sbercrm-doc-template-service-impl`.
