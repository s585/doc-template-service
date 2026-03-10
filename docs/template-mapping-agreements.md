# Соглашения по Template Mapping

Дата: 2026-03-10

## Назначение

В документе зафиксированы текущие договоренности по `TemplateMapping` и связанным DTO.
До появления новой проектной заметки этот файл считается источником истины для реализации.

## TemplateMapping

```json
{
  "id": "uuid",
  "key": "string",
  "scope": "FILE_NAME | VALUE | TABLE",
  "value": {
    "type": "STRING | NUMBER | DATE | DATETIME | BOOLEAN",
    "expression": {},
    "source": {
      "kind": "DIRECT | REFERENCE | CONSTANT"
    }
  }
}
```

## Scope

- `FILE_NAME`: значение используется при формировании имени файла
- `VALUE`: обычное одиночное значение для подстановки в шаблон
- `TABLE`: значение резолвится для каждого объекта в табличном/списочном контексте

## Тип значения

- `STRING`
- `NUMBER`
- `DATE`
- `DATETIME`
- `BOOLEAN`

Для `TABLE` поле `value.type` описывает тип одного резолвленного элемента, а не всей коллекции.

## Expression

`expression` - это отдельное дерево выражений, не `RuleDto`.

Общие правила:

- `expression` выполняется строго после `source`
- внутри контекста expression доступен только `$value`
- прямой доступ к `source.*`, `reference.*`, `targetPath` запрещен
- fallback выражается через `coalesce`
- результат expression должен быть совместим с `value.type`
- для `TABLE` в рамках MVP поддерживаются только поэлементные операции

Форма узла expression:

```json
{
  "op": "string",
  "type": "operation | primitive",
  "args": []
}
```

Поддерживаемые в MVP операторы:

- `coalesce`
- `formatDate`
- `upper`
- `lower`
- `trim`

## Source

Поддерживаемые `source.kind`:

- `DIRECT`
- `REFERENCE`
- `CONSTANT`

### DIRECT

```json
{
  "kind": "DIRECT",
  "path": "source.doc_number"
}
```

### CONSTANT

```json
{
  "kind": "CONSTANT",
  "value": "literal"
}
```

`value` хранится как типизированный JSON literal. Для `DATE` и `DATETIME` literal передается строкой в согласованном формате.

### REFERENCE

```json
{
  "kind": "REFERENCE",
  "targetPath": "source.document$c.dealProduct$c",
  "entityId": "uuid",
  "referenceFieldName": "document$c",
  "referenceValuePath": "source.document$c.id",
  "path": "reference.product.name",
  "sort": [],
  "paging": {
    "page": 0,
    "size": 100
  }
}
```

Семантика:

- `referenceValuePath` читается из исходного объекта
- фильтр запроса строится как `referenceFieldName = resolved(referenceValuePath)`
- связанные объекты загружаются через `DataClient.getListObjectsV3(...)`
- `path` резолвится из каждого загруженного reference-объекта
- если объекты не найдены, результатом является пустой список
- вложенные обратные ссылки не поддерживаются
- `targetPath` недоступен внутри expression-контекста

Логический путь результата после обогащения:

```text
targetPath + "." + path without "reference."
```

## Валидация

На уровне API необходимо проверять:

- синтаксис путей
- обязательные поля для каждого `source.kind`
- допустимые путевые префиксы для текущего `source.kind`
- совместимость `expression` и результирующего типа
- некорректные `path` и `referenceValuePath`

Стратегия обработки runtime-ошибок пока не зафиксирована.

## Примеры

Обычное поле:

```json
{
  "id": "uuid",
  "key": "deal_num",
  "scope": "VALUE",
  "value": {
    "type": "STRING",
    "expression": null,
    "source": {
      "kind": "DIRECT",
      "path": "source.doc_number"
    }
  }
}
```

Reference через lookup:

```json
{
  "id": "uuid",
  "key": "storage_product_name",
  "scope": "TABLE",
  "value": {
    "type": "STRING",
    "expression": null,
    "source": {
      "kind": "REFERENCE",
      "targetPath": "source.document$c.dealProduct$c",
      "entityId": "uuid",
      "referenceFieldName": "document$c",
      "referenceValuePath": "source.document$c.id",
      "path": "reference.product.name",
      "sort": [],
      "paging": {
        "page": 0,
        "size": 100
      }
    }
  }
}
```
