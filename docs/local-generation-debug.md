# Локальная генерация без внешнего file-storage

Для локальной разработки сервис может пройти штатный путь генерации без S3:

1. импорт шаблона через `/v1/doc/template/import`;
2. сохранение файла шаблона в локальный `FileStorageGatewayStub`;
3. запуск генерации через `/v1/doc/generated`;
4. сохранение результата в тот же локальный stub-storage;
5. поиск итогового файла на диске по `s3Key`.

Локальный режим включается профилем `local`. Все настройки локального
file-storage лежат вместе в `application-local.yml`:

```yaml
saas:
  doc-template:
    file-storage:
      stub-enabled: true
      folder: "templates"
      stub-root-path: "${user.dir}"
```

Запустить сервис локально можно с активным профилем:

```bash
mvn -pl saas-sbercrm-doc-template-service-impl spring-boot:run \
  -Dspring-boot.run.profiles=local
```

При импорте и успешной генерации сервис пишет в лог и storage key, и локальный
абсолютный путь файла:

```text
Uploaded template file: ..., fileKey=..., filePath=...
Completed generation job: ..., fileKey=..., filePath=...
```

## Запуск сценария

Импортировать шаблон:

```bash
curl -X POST 'http://localhost:8080/v1/doc/template/import' \
  -H 'X-Tenant-Id: 11111111-1111-1111-1111-111111111111' \
  -H 'X-User-Id: 22222222-2222-2222-2222-222222222222' \
  -F 'request={
    "entityId": "34784e52-325b-11f1-95d9-005056aabc43",
    "name": "Договор поставки",
    "description": "Локальная отладка",
    "code": "SUPPLY_CONTRACT_LOCAL"
  };type=application/json' \
  -F 'file=@{pathToTemplate}'
```

Запустить генерацию:

```bash
curl -X POST 'http://localhost:8080/v1/doc/generated' \
  -H 'Content-Type: application/json' \
  -H 'X-Tenant-Id: 11111111-1111-1111-1111-111111111111' \
  -H 'X-User-Id: 22222222-2222-2222-2222-222222222222' \
  -d '{
    "templateId": "<templateId из ответа import>",
    "entityId": "34784e52-325b-11f1-95d9-005056aabc43",
    "objectId": "e1fff8d5-7a9b-4de8-88a3-961b13daa700",
    "requestId": "77777777-7777-7777-7777-777777777700",
    "formats": ["DOCX"]
  }'
```

Получить документ и взять `files[].s3Key`:

```bash
curl 'http://localhost:8080/v1/doc/<documentId>' \
  -H 'X-Tenant-Id: 11111111-1111-1111-1111-111111111111'
```

Посмотреть, где этот ключ лежит на локальном диске:

```bash
curl --get 'http://localhost:8080/internal/dev/file-storage/file' \
  --data-urlencode 'key=<files[].s3Key>'
```

Ответ содержит:

```json
{
  "key": "documents/.../result.docx",
  "rootPath": "{projectRoot}",
  "path": "{projectRoot}/documents/.../result.docx",
  "exists": true,
  "regularFile": true,
  "sizeBytes": 12345
}
```

Локально импортированные шаблоны лежат в
`{projectRoot}/templates/{entityId}/`, а результаты генерации в
`{projectRoot}/documents/{entityId}/{objectId}/{documentId}/`.

`/internal/dev/file-storage/file` активен только при
`saas.doc-template.file-storage.stub-enabled=true`, что в штатной локальной
разработке задается профилем `local`. Если включен настоящий file-storage,
endpoint не поднимается.

## Ограничение

Этот путь убирает зависимость от S3/file-storage, но не подменяет
`BusinessObjectGateway`. Если mappings используют `DIRECT` или `REFERENCE`,
локальный запуск все еще должен иметь доступ к core-client либо к локальному
стабу этого клиента.
