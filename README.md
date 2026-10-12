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

Создайте `.env` в корне проекта и задайте `POSTGRES_PASSWORD`, `ADMIN_EMAIL`, `ADMIN_PASSWORD` (от 6 символов) и `S3_SECRET_KEY`. При необходимости задайте `POSTGRES_DB`, `POSTGRES_USER` и `S3_ACCESS_KEY`; для них есть значения по умолчанию. Затем выполните `docker compose --env-file .env up --build -d`.

API слушает `http://localhost:9999`. PostgreSQL слушает `15432` снаружи и внутри контейнера. MinIO API доступен на `127.0.0.1:19000`, консоль — на `127.0.0.1:19001`; внутри сети Docker приложение использует `minio:9000`. Dockerfile собирает приложение без выполнения тестов.

## Swagger и авторизация

Swagger UI: `http://localhost:9999/swagger-ui.html`. OpenAPI JSON: `http://localhost:9999/v3/api-docs`. Каждый REST-контроллер и его маршруты описаны в Swagger. Публичные операции доступны без пароля; для `/api/admin/**` используется HTTP Basic с `ADMIN_EMAIL` и `ADMIN_PASSWORD` из `.env`. В Swagger нажмите **Authorize** и введите эти данные.

## Передача заявок в amoCRM

После OAuth-подключения каждая новая заявка сайта создаёт сделку в воронке `Заявки с сайта` на этапе `Новая заявка` и связанный контакт с телефоном и/или почтой. Текст обращения и предпочтительный способ связи добавляются примечанием к сделке. Заявка в любом случае сначала сохраняется в PostgreSQL. Состояние отправки отображается в `GET /api/admin/leads`; ошибки повторяются автоматически.

Используется внешняя интеграция amoCRM: её OAuth-реквизиты передаются кнопкой подключения на сервер, поэтому вручную задавать `AMOCRM_CLIENT_ID` и `AMOCRM_CLIENT_SECRET` не нужно. Добавьте в `.env` постоянный `AMOCRM_TOKEN_ENCRYPTION_KEY`, сгенерированный командой `openssl rand -base64 32`; не меняйте его после первого успешного подключения, иначе сохранённые токены невозможно будет расшифровать. Затем добавьте все amoCRM location из `deploy/nginx-location.conf.example` в HTTPS server block `itwebs.ru`, проверьте `nginx -t` и перезагрузите Nginx. После деплоя откройте `https://itwebs.ru/api/admin/integrations/amocrm/authorize`, войдите с HTTP Basic данными администратора, нажмите кнопку и разрешите доступ аккаунту `itwebs.amocrm.ru`. amoCRM передаст OAuth-реквизиты на `/api/integrations/amocrm/secrets`, затем вернётся на `/api/integrations/amocrm/callback`; backend сохранит их зашифрованными.

Новые заявки, полученные до OAuth-подключения, автоматически не переносятся. Укажите другую воронку или этап через `AMOCRM_PIPELINE_NAME` и `AMOCRM_STATUS_NAME`. Назначение менеджера необязательно и задаётся через `AMOCRM_RESPONSIBLE_USER_ID`.

## Маршруты

| Метод | Путь | Доступ | Назначение |
| --- | --- | --- | --- |
| GET | `/api/content?section=...` | публичный | опубликованный контент |
| GET | `/api/content/{id}` | публичный | один опубликованный элемент |
| GET | `/api/pages/services` | публичный | готовый ответ страницы услуг с общими блоками и связанными кейсами |
| GET | `/api/pages/product` | публичный | страница продукта с демонстрацией ProBuilder и связанными проектами |
| GET | `/api/pages/outstaff` | публичный | страница аутстаффинга со специалистами, фильтрами и общей командой |
| GET | `/api/pages/cases` | публичный | готовый ответ страницы кейсов с фильтрами и вложенными проектами |
| GET | `/api/media/{id}` | публичный | файл изображения |
| POST | `/api/leads` | публичный | сохранить заявку |
| GET, POST | `/api/admin/content` | админ | список всех элементов, создание |
| GET, PUT, DELETE | `/api/admin/content/{id}` | админ | просмотр, изменение, удаление |
| GET, POST | `/api/admin/media` | админ | список файлов, загрузка multipart |
| GET, PUT, DELETE | `/api/admin/media/{id}` | админ | метаданные, alt-текст, удаление |
| GET | `/api/admin/leads` и `/api/admin/leads/{id}` | админ | просмотр заявок |
| PATCH, DELETE | `/api/admin/leads/{id}` | админ | статус и удаление |

Фронтенд страниц услуг и кейсов запрашивает один маршрут на страницу. Ответ содержит `page` и объект `sections`; ключи секций — имена контентных разделов, значения — группы записей с полями по `itemKey`. В карточках кейсов поле `project` уже содержит связанную карточку проекта, а в карточках страницы услуг она доступна в `services-page-project-items`. Категории фильтров кейсов находятся в `cases-page-filters`; у кейса может быть несколько полей `filter1`, `filter2` и т. д. Общие шапка, форма и подвал входят в ответ каждой страницы. Редактирование по-прежнему выполняется через универсальные `/api/admin/content` и `/api/admin/media` маршруты.

При первом запуске или обновлении старой базы однократно создаются недостающие элементы контента по десктопному макету: шапка, первый экран, бренды, услуги, проекты, показатели, технологии, аутстаффинг, команда, форма связи, SEO-блок и подвал. Уже изменённые элементы не перезаписываются; после заполнения удалённые администратором элементы не восстанавливаются при перезапуске. Каждый текст, ссылка, значок и фотография представлены отдельным элементом; карточки объединяются через `groupName`. `sortOrder` задаёт порядок. Фоновые градиенты и оформление не являются данными backend.

Отдельные исходные картинки из Figma не предоставлены: загрузите их через `POST /api/admin/media`, затем укажите `mediaId` в нужном элементе типа `IMAGE` через `PUT /api/admin/content/{id}`. Для изображения также можно задать `href` и альтернативный текст (`value`). Публичный API возвращает URL изображения в `mediaUrl`. Админского HTML-интерфейса нет; редактирование доступно через Swagger.

Для продакшена нужны HTTPS, резервные копии PostgreSQL и бакета, а также миграции схемы вместо текущего `ddl-auto: update`.

Настройка автоматического деплоя по GitHub webhook описана в [deploy/README.md](deploy/README.md).

## Страница Outstaff

`GET /api/pages/outstaff` возвращает опубликованные данные страницы. Все её тексты, ссылки, карточки и принадлежность специалистов к фильтрам редактируются через общий CRUD `/api/admin/content`. Создание: POST; чтение списка: GET; чтение, обновление и удаление по ID: GET, PUT, DELETE `/api/admin/content/{id}`. Данные и ID текущих полей видны в ответе страницы. PUT принимает полный ContentRequest, включая sectionName, itemKey, groupName, type, label, value, href, mediaId, sortOrder, published.

| sectionName | Содержимое |
| --- | --- |
| outstaff-page | Первый экран, ссылки на специалистов и форму |
| outstaff-page-use-cases | Заголовок блока ситуаций |
| outstaff-page-use-case-items | Пять ситуаций: tag, title, description |
| outstaff-page-specialists | Заголовок и описание специалистов |
| outstaff-page-filters | Шесть фильтров: key, label |
| outstaff-page-specialist-items | Шесть карточек: direction, grade, grade2 (при наличии), title, skills, price, filterN, cta |
| team, team-members | Общий заголовок команды, имена, должности и IMAGE-поля photo |
| outstaff-page-team-overrides | Имя и должность member-6 на этой странице; фото берётся из team-members |
| outstaff-page-team | Счётчик команды и подпись |
| outstaff-page-process | Заголовок процесса |
| outstaff-page-process-steps | Пять этапов: number, title, description |
| outstaff-page-terms | Заголовок условий оплаты |
| outstaff-page-term-items | Три условия: title, description |
| contact | Общие надписи формы |
| outstaff-page-contact | Надпись выбора специалиста, грейда, подсказка задачи |
| outstaff-page-contact-specialists | Шесть вариантов специалиста: label |
| outstaff-page-contact-grades | junior, middle, senior: label |
| outstaff-page-seo | Надзаголовок, заголовок, два абзаца и подзаголовок |
| outstaff-page-seo-items | Четыре пункта: label |

Фильтрация на фронтенде: для `all` показать все карточки; для остальных сопоставить key фильтра со значениями filter1, filter2, ... карточки. Связи проверены по переключению вкладок сайта: web — специалисты 1–4; mobile — 1–3; automation — 5–6; crm — 4–5; 1c — 4 и 6.

Для команды применяйте поля outstaff-page-team-overrides поверх team-members по совпадающему groupName. Шесть фотографий уже представлены общими IMAGE-полями team-members/member-1…member-6/photo. Новые дубли файлов не создаются. Если mediaId отсутствует, файл нужно загрузить через POST /api/admin/media и привязать через PUT /api/admin/content/{id}. Ответ содержит mediaUrl после привязки.

Сидер Outstaff добавляет отсутствующие поля один раз, не перезаписывает существующие данные и не восстанавливает удалённые элементы после завершения заполнения. Форма остаётся подключённой к существующему POST /api/leads; выбранного специалиста и грейды фронтенд может включить в текст задачи.

## Страница Product

`GET /api/pages/product` возвращает готовый контент Product. Редактирование каждого поля: общий CRUD `/api/admin/content` (GET/POST для коллекции, GET/PUT/DELETE по `{id}`). Ответ страницы содержит ID полей. Общие header, contact и footer включаются автоматически. Сидер Product добавляет недостающие записи однократно, сохраняет пользовательские изменения и после завершения не восстанавливает удалённые поля.

| sectionName | Поля / содержимое |
| --- | --- |
| product-page | eyebrow, title, description первого экрана |
| product-page-demo | Все надписи HTML-демонстрации ProBuilder, включая меню, домен, индексацию, подписку, контакты и alt-подписи |
| product-page-advantages | eyebrow, title, description |
| product-page-advantage-items | Четыре карточки: number, title, description |
| product-page-control | eyebrow, title, description |
| product-page-control-items | Три карточки: number, title, description |
| product-page-use-cases | eyebrow, title, description |
| product-page-use-case-items | Три примера: tag, title, description, projectGroup, cta; imageMobile у первых двух; projectTitle/projectDescription у Vesna |
| product-page-stages | eyebrow, title, description |
| product-page-stage-items | Три этапа: label, title, description |
| product-page-audience | eyebrow, title |
| product-page-audience-items | Три карточки: title, description |
| product-page-contact | hint, LINK services; остальные надписи берутся из contact |
| product-page-seo | eyebrow |
| services-page-seo, services-page-seo-topics | Общие с Services заголовок, абзацы и пять тематических пунктов SEO |

Карточки `product-page-use-case-items` ссылаются на `project-cards`: Learn English (`project-12`, новый проект), Focus (`project-5`) и Vesna (`project-8`). Публичный ответ уже содержит связанную запись в `project`. Локальные поля projectTitle/projectDescription заменяют отображаемое название/описание на Product, остальные данные остаются общими с кейсами.

Изображения: десктопная картинка — `project.fields.image`; мобильная — `fields.imageMobile`, если она загружена, иначе десктопная. Learn English имеет новый IMAGE-слот project-cards/project-12/image, а первые две карточки — отдельные IMAGE-слоты imageMobile, поскольку на сайте используются разные файлы для размеров экрана. Vesna использует одну общую фотографию. Загружайте файл через POST /api/admin/media, затем привязывайте его mediaId через PUT /api/admin/content/{id}. До загрузки mediaUrl равен null. Файлы сайта автоматически в MinIO не импортируются.

ProBuilder на сайте нарисован HTML-компонентами, а не готовой картинкой: все 47 уникальных надписей и alt-подписей доступны в product-page-demo. Одинаковые подписи в разных местах демонстрации используют одно поле. Даты, цена и контакты в демонстрации — рекламные примеры, а не действующие реквизиты или подписка backend. Вёрстку и отображение данных из API подключает фронтенд.

## Результат повторной сверки Outstaff и Product

Проверены тексты всех блоков, ссылки, изображения, варианты формы, фильтры специалистов и надписи HTML-демонстрации по текущим страницам сайта. Дополнительный сидер PageCoverageContent добавляет 22 отсутствовавших поля и исправляет 21 ссылку, если значение и href ещё совпадают с исходными настройками. Правки администратора сохраняются. Маркер pageCoverageSeedV1 предотвращает повторное заполнение после удаления полей.

Дополнительно доступны:

- `outstaff-page-meta`, `product-page-meta`: title и description для SEO и вкладки браузера.
- `outstaff-page/floatingCta`, `product-page/floatingCta`: плавающая кнопка со ссылкой на `/#lead-form`.
- `header/navProduct`, `header/max`, `header/maxIcon`, `header/home`, `footer/home`: недостающие элементы общей шапки и ссылки логотипов.
- `outstaff-page-contact-specialists`: key + label (`ux-ui`, `frontend`, `backend`, `devops`, `qa`, `analyst`).
- `outstaff-page-contact-grades`: key + label для junior, middle, senior.
- `outstaff-page-team-overrides/member-6/photoAlt`: alt фотографии Алёши.
- `contact/consentPrefix`: текст перед ссылкой на политику; `consent` остаётся полной подписью для доступности.

Сопоставление исходных изображений (загрузить через media CRUD; файлов в MinIO сидер не создаёт):

| Поле | Исходный файл сайта |
| --- | --- |
| team-members/member-1/photo, member-4/photo | /home/team/darina.png |
| team-members/member-2/photo | /home/team/evgeniy.png |
| team-members/member-3/photo | /home/team/svetlana.png |
| team-members/member-5/photo, member-6/photo | /home/team/stanislav.png |
| project-cards/project-12/image | /home/works/ai-assistant.png (именно эта картинка показана у Learn English на сайте) |
| product-page-use-case-items/use-case-1/imageMobile | /product/usecases/learn-english.webp |
| project-cards/project-5/image | /home/works/focus-app.png |
| product-page-use-case-items/use-case-2/imageMobile | /product/usecases/focus.webp |
| project-cards/project-8/image | /product/usecases/vesna.webp |

Повторяющиеся файлы можно загрузить один раз и использовать один mediaId. Иконки и логотипы имеют общие IMAGE-поля; геометрия SVG, фоновые градиенты, расположение и анимации остаются частью фронтенда.

Форма Outstaff на текущем сайте не содержит поля задачи. Для существующего POST /api/leads фронтенд должен сформировать обязательный `message` из выбранного специалиста и грейдов. Product имеет textarea с HTML name=task; в запрос API его текст передаётся как `message`. Канал связи в обеих формах передаётся как `contactMethod`, а не HTML name=channel. Проверка не отправляла реальные заявки.

Сверка относится к локальному backend. При проверке 2026-10-12 публичные `/api/pages/outstaff` и `/api/pages/product` на порту 9999 отвечали HTTP 401; наличие созданных полей в серверной базе этим ответом не подтверждено. Публикация изменений и подключение фронтенда к новым данным остаются отдельным шагом.
