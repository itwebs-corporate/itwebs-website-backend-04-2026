# ITWEBS API

Только backend: Java 21, Spring Boot, PostgreSQL, S3-совместимое хранилище MinIO и Swagger/OpenAPI. Данные главной страницы из макета хранятся как отдельные редактируемые элементы: текст, ссылка или изображение. HTML-страниц и админского интерфейса в проекте нет.

## Структура

```text
src/main/java/ru/itwebs/cms/
├── bootstrap/    # начальные элементы контента по макету
├── config/       # HTTP Basic и OpenAPI
├── controller/   # REST-маршруты и обработка ошибок
├── dto/          # JSON-запросы и ответы
├── entity/       # JPA-сущности
├── repository/   # PostgreSQL
└── service/      # бизнес-логика и работа с S3
```

## Запуск

Если `.env` отсутствует, скопируйте `.env.example` в `.env` и задайте сильные `POSTGRES_PASSWORD`, `ADMIN_PASSWORD` (от 16 символов) и `S3_SECRET_KEY`. Затем выполните `docker compose --env-file .env up --build -d`.

API слушает `http://localhost:9999`. PostgreSQL слушает `15432` снаружи и внутри контейнера. MinIO API доступен на `127.0.0.1:19000`, консоль — на `127.0.0.1:19001`; внутри сети Docker приложение использует `minio:9000`. Dockerfile собирает приложение без выполнения тестов.

## Swagger и авторизация

Swagger UI: `http://localhost:9999/swagger-ui.html`. OpenAPI JSON: `http://localhost:9999/v3/api-docs`. Каждый REST-контроллер и его маршруты описаны в Swagger. Публичные операции доступны без пароля; для `/api/admin/**` используется HTTP Basic с `ADMIN_EMAIL` и `ADMIN_PASSWORD` из `.env`. В Swagger нажмите **Authorize** и введите эти данные.

## Маршруты

| Метод | Путь | Доступ | Назначение |
| --- | --- | --- | --- |
| GET | `/api/content?section=...` | публичный | опубликованный контент |
| GET | `/api/content/{id}` | публичный | один опубликованный элемент |
| GET | `/api/media/{id}` | публичный | файл изображения |
| POST | `/api/leads` | публичный | сохранить заявку |
| GET, POST | `/api/admin/content` | админ | список всех элементов, создание |
| GET, PUT, DELETE | `/api/admin/content/{id}` | админ | просмотр, изменение, удаление |
| GET, POST | `/api/admin/media` | админ | список файлов, загрузка multipart |
| GET, PUT, DELETE | `/api/admin/media/{id}` | админ | метаданные, alt-текст, удаление |
| GET | `/api/admin/leads` и `/api/admin/leads/{id}` | админ | просмотр заявок |
| PATCH, DELETE | `/api/admin/leads/{id}` | админ | статус и удаление |

При первом запуске создаются текстовые данные и места для изображений по макету. Отдельные исходные картинки из Figma не предоставлены: загрузите их через `POST /api/admin/media`, затем укажите `mediaId` в нужном элементе контента через `PUT /api/admin/content/{id}`. Группа связывает поля одной карточки, а `sortOrder` задаёт порядок.

Для продакшена нужны HTTPS, резервные копии PostgreSQL и бакета, а также миграции схемы вместо текущего `ddl-auto: update`.

Настройка автоматического деплоя по GitHub webhook описана в [deploy/README.md](deploy/README.md).
