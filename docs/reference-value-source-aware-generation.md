# Изменения ветки `codex/reference-value-source-aware-generation`

Дата: 2026-05-13

## Назначение

Документ фиксирует архитектурные изменения, внесенные коммитом
`08e0c5f3a57b26e887cb06f1f8bee6423b2191f8` в ветке
`codex/reference-value-source-aware-generation`.

Основной архитектурный source of truth по сервису остается в
[docs/doc-template-service-architecture.md](/Users/asvetov/Projects/document-template/docs/doc-template-service-architecture.md),
а этот файл описывает состав и мотивацию конкретного эволюционного шага.

## Что изменилось

### 1. `TABLE` заменен на `COLLECTION`

Scope `TABLE` больше не отражал реальную семантику runtime, потому что
повторяющиеся блоки поддерживаются не только для табличных строк `DOCX`, но и
для list items, а также для строк `XLSX`.

Итоговая модель scope:

- `VALUE`
- `COLLECTION`
- `FILE_NAME`

### 2. Generation context стал row-based для коллекций

Вместо column-based структуры коллекций generation runtime перешел на
dataset-модель:

- `GenerationTemplateContext.scalarValues`
- `GenerationTemplateContext.collections`
- `GenerationTemplateContext.generatedFileName`

Коллекция представлена как `CollectionDataset`:

- `queryKey` — идентификатор конкретного lookup;
- `keys` — placeholder keys, принадлежащие dataset-у;
- `rows` — `List<Map<String, String>>`.

Это выровняло runtime-модель с тем, как данные реально рендерятся в `DOCX` и
`XLSX`: построчно, а не поколоночно.

### 3. `REFERENCE` для коллекций вынесен в отдельный resolver-слой

Логика reference lookup больше не живет внутри assembler’а.

Вместо этого:

- `GenerationMappingPlanner` строит план обработки mappings;
- `GenerationCollectionDatasetResolver` и `CollectionDatasetResolver`
  отвечают за collection-ветку;
- `ReferenceCollectionDatasetResolver` инкапсулирует reference-specific
  lookup, paging и сбор row-based dataset-а.

Assembler остался orchestration-слоем и перестал знать детали
`REFERENCE`-источника.

### 4. Постраничная обработка reference business objects

Для reference lookup добавлен page iterator:

- `BusinessObjectGateway` загружает одну страницу;
- общий `PageIterator` выполняет paging;
- page-by-page обработка сразу проецирует raw business objects в dataset rows.

Это убрало дублирующие lookup-циклы по нескольким mapping keys одной и той же
reference-сущности и снизило риск избыточного потребления памяти.

### 5. Repeat-рендер добавлен в `DOCX` и `XLSX`

Коллекции теперь рендерятся структурно:

- `DOCX` table row -> repeat-unit;
- `DOCX` list item -> repeat-unit;
- `XLSX` row -> repeat-unit.

Рендер повторяемых блоков строится на dataset matching по placeholder keys.

Поддержанные правила:

- внутри repeat-блока допускается только один dataset;
- scalar placeholders могут присутствовать внутри repeat-блока;
- пустая коллекция удаляет шаблонный block/row;
- неоднозначный dataset и частично незаполненный repeat-блок завершают
  generation ошибкой.

### 6. Семантическая валидация mappings вынесена в отдельный validator

Для финальной конфигурации шаблона добавлен `TemplateMappingValidator`.

Ключевые правила:

- `REFERENCE` допустим только с `COLLECTION`;
- `COLLECTION` с заполненным source допустим только для `REFERENCE`;
- `generated_file_name` допустим только с `FILE_NAME`;
- `generated_file_name` не может использовать `REFERENCE`.

## Почему это важно

Этот шаг перевел generation runtime от разрозненных scalar substitutions к
согласованной dataset-модели повторяющихся блоков.

Практический результат:

- один reference lookup обслуживает всю группу collection mappings;
- коллекции рендерятся одинаково по смыслу в `DOCX` и `XLSX`;
- generation context стал ближе к layout semantics шаблона;
- validator и runtime получили более четкие и предсказуемые правила.

## Ограничения после этого шага

- `COLLECTION` по-прежнему не поддерживает multi-paragraph/multi-row repeat block
  как одну логическую единицу;
- для `XLSX` формулы в размноженных строках копируются как есть и требуют
  отдельного улучшения, если понадобится корректировка относительных ссылок;
- extraction/import semantics для `XLSX` и переход к `suggestedScope` выделены
  в отдельный follow-up.
