package ru.itwebs.cms.bootstrap;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import ru.itwebs.cms.entity.ContentItem;
import ru.itwebs.cms.repository.ContentRepository;

import java.util.HashSet;
import java.util.Set;

/** Seeds the page-specific copy observed on /services. Shared footer, contact form,
 * categories and project cards use the existing site-wide content sections. */
@Configuration
public class ServicesPageContent {
    @Bean
    @Order(2)
    CommandLineRunner seedServicesPage(ContentRepository repository) {
        return args -> {
            if (repository.existsBySectionNameAndItemKeyAndGroupName("_system", "servicesPageSeedV2", null)) return;
            Writer w = new Writer(repository);

            w.text("services-page", "eyebrow", "# Всё, что нужно для вашей задачи");
            w.text("services-page", "title", "Услуги ITWEBS — полный цикл разработки");
            w.text("services-page", "description", "Повышаем технологический потенциал крупного бизнеса. За 6+ лет работы мы накопили глубокий опыт в разработке ПО и интеграции сложных ИТ-систем.");

            // The shared category tabs already live in service-categories.
            Card[][] catalog = {
                    {
                            new Card("Корпоративный сайт под ключ", "от 350.000 ₽", "Структура, дизайн и вёрстка под ваши задачи. Подключаем аналитику и передаём проект с документацией."),
                            new Card("Интернет-магазин с оплатой и доставкой", "от 700.000 ₽", "Каталог, корзина, эквайринг и службы доставки. Синхронизируем остатки и цены с вашей учётной системой."),
                            new Card("Веб-портал и личный кабинет", "от 900.000 ₽", "Роли, права и сценарии самообслуживания. Выдерживаем нагрузку и разграничиваем доступ к данным."),
                            new Card("Лендинг под рекламную кампанию", "от 180.000 ₽", "Быстрая страница с формой заявки и сквозной аналитикой. Собираем за две недели вместе с текстами."),
                            new Card("Редизайн и ускорение сайта", "от 250.000 ₽", "Обновляем интерфейс, чиним вёрстку и разгоняем показатели Core Web Vitals без потери позиций в поиске.")
                    },
                    {
                            new Card("Разработка мобильного приложения для Android", "от 400.000 ₽", "Android-приложения на Kotlin и Java. Оптимизируем для всех устройств и экономим бюджет за счёт продуманной архитектуры."),
                            new Card("Разработка мобильного приложения для iOS", "от 400.000 ₽", "iOS-приложения с нативной производительностью. Используем Swift и новейшие технологии Apple для безупречного пользовательского опыта."),
                            new Card("Разработка мобильного приложения (iOS + Android)", "от 400.000 ₽", "Разработка кроссплатформенного мобильного приложения на одной кодовой базе."),
                            new Card("Разработка PWA приложений", "от 400.000 ₽", "Прогрессивные веб-приложения (PWA), которые увеличивают продажи. Работают офлайн, устанавливаются на экран смартфона и грузятся за секунды."),
                            new Card("Доработка и оптимизация работы мобильного приложения", "от 400.000 ₽", "Ускоряем загрузку, снижаем краши и повышаем рейтинг в магазинах.")
                    },
                    {
                            new Card("Автоматизация внутренних процессов", "от 500.000 ₽", "Описываем маршруты заявок и согласований, переносим их из таблиц в систему с уведомлениями и сроками."),
                            new Card("Электронный документооборот", "от 450.000 ₽", "Шаблоны документов, маршруты подписания и хранение версий. Подключаем ЭЦП и журнал действий."),
                            new Card("Боты и уведомления для сотрудников", "от 200.000 ₽", "Telegram- и MAX-боты для заявок, отчётов и напоминаний. Забираем рутину из переписки в сценарии."),
                            new Card("Дашборды и отчётность", "от 300.000 ₽", "Собираем данные из разных систем в одну витрину: планы, факты и отклонения в реальном времени."),
                            new Card("Складской и производственный учёт", "от 600.000 ₽", "Терминалы сбора данных, штрихкоды и остатки онлайн. Сокращаем пересорт и ручные сверки.")
                    },
                    {
                            new Card("Внедрение CRM под ваши процессы", "от 400.000 ₽", "Настраиваем воронки, права и отчёты. Переносим базу клиентов и обучаем отдел продаж."),
                            new Card("Разработка собственной CRM", "от 1.200.000 ₽", "Когда коробочные решения не подходят: своя логика сделок, расчётов и интеграций с вашими сервисами."),
                            new Card("Интеграция телефонии и мессенджеров", "от 250.000 ₽", "Звонки, чаты и заявки с сайта падают в одну карточку клиента с записью разговора и историей."),
                            new Card("Сквозная аналитика продаж", "от 350.000 ₽", "Считаем стоимость заявки и окупаемость каналов: реклама, CRM и финансы в одном отчёте."),
                            new Card("Миграция с другой CRM", "от 300.000 ₽", "Переносим сделки, файлы и историю без остановки продаж, сверяем данные до и после переезда.")
                    },
                    {
                            new Card("Интеграция 1С с сайтом", "от 300.000 ₽", "Обмен товарами, ценами, остатками и заказами. Настраиваем расписание и контроль ошибок обмена."),
                            new Card("Обмен 1С и CRM", "от 350.000 ₽", "Сделки, счета и оплаты синхронно в двух системах. Менеджер видит статус документа без переключений."),
                            new Card("Интеграция с маркетплейсами", "от 400.000 ₽", "Wildberries, Ozon и Яндекс Маркет: заказы, остатки и статусы отгрузок приходят прямо в 1С."),
                            new Card("Подключение ЭДО и маркировки", "от 250.000 ₽", "Настраиваем обмен документами с контрагентами и работу с «Честным знаком» без ручного ввода."),
                            new Card("Доработка конфигураций 1С", "от 200.000 ₽", "Отчёты, печатные формы и обработки под ваш учёт. Сохраняем возможность типовых обновлений.")
                    },
                    {
                            new Card("Выделенная команда разработки", "от 350.000 ₽ / мес", "Собираем команду под задачу за две недели: разработчики, тестировщик, аналитик и тимлид."),
                            new Card("Frontend-разработчик в ваш проект", "от 320.000 ₽ / мес", "React, Next.js и TypeScript. Специалист работает в вашем трекере и по вашим процессам."),
                            new Card("Backend-разработчик в ваш проект", "от 350.000 ₽ / мес", "Node.js, Python и Go. Проектируем API, выдерживаем нагрузку и покрываем код тестами."),
                            new Card("DevOps-инженер", "от 380.000 ₽ / мес", "CI/CD, мониторинг и инфраструктура как код. Сокращаем время выкладки и простои сервиса."),
                            new Card("Тестировщик и автотесты", "от 280.000 ₽ / мес", "Ручное и автоматизированное тестирование. Настраиваем регресс, чтобы релизы не ломали прод.")
                    }
            };
            String[] categoryKeys = {"web", "mobile", "automation", "crm", "1c", "outstaff"};
            String[] categoryNames = {"Web разработка", "Mobile разработка", "Цифровая автоматизация", "Интеграция и разработка CRM систем", "Услуги в области 1С интеграций", "Аутстаффинг IT специалистов"};
            for (int c = 0; c < catalog.length; c++) {
                for (int i = 0; i < catalog[c].length; i++) {
                    Card card = catalog[c][i];
                    String group = categoryKeys[c] + "-service-" + (i + 1);
                    w.groupText("services-page-catalog", group, "category", categoryNames[c]);
                    w.groupText("services-page-catalog", group, "title", card.title());
                    w.groupText("services-page-catalog", group, "price", card.price());
                    w.groupText("services-page-catalog", group, "description", card.description());
                    w.groupLink("services-page-catalog", group, "projects", "Проекты", "/cases");
                    w.groupLink("services-page-catalog", group, "details", "Подробнее", "/contacts");
                }
            }

            String[][] stacks = {
                    {"Веб-разработка", "Создаём сайты, магазины и порталы на современном стеке HTML5, CSS3 и JavaScript — быстрые, надёжные и готовые к росту вашего бизнеса.", "HTML5", "CSS", "JavaScript"},
                    {"Разработка мобильных приложений", "Создание приложений: натив (iOS/Android) и Flutter. Оптимизация под все типы устройств. Экономия бюджета за счет умных архитектурных решений.", "Java", "Kotlin", "Swift", "Objective-C", "Flutter", "HTML5", "CSS", "JavaScript"},
                    {"Цифровая автоматизация процессов", "Автоматизируем рутину и связываем сервисы компании: боты, документооборот, дашборды и интеграции на JavaScript.", "JavaScript", "HTML5", "CSS"},
                    {"Интеграция и разработка CRM систем", "Настраиваем и дорабатываем CRM, связываем их с сайтом и внутренними сервисами компании."},
                    {"Услуги в области 1С интеграций", "Интегрируем 1С с сайтом и внешними сервисами, автоматизируем обмен данными и отчётность."},
                    {"Аутстаффинг IT-специалистов", "Усиливаем вашу команду разработчиками нужного стека — от нативной мобильной разработки до веба."}
            };
            for (int s = 0; s < stacks.length; s++) {
                String group = categoryKeys[s];
                w.groupText("services-page-stacks", group, "title", stacks[s][0]);
                w.groupText("services-page-stacks", group, "description", stacks[s][1]);
                for (int i = 2; i < stacks[s].length; i++) {
                    String technology = stacks[s][i];
                    String techGroup = group + "-technology-" + (i - 1);
                    w.groupText("services-page-stack-items", techGroup, "name", technology);
                    w.image("services-page-stack-items", techGroup, "logo", "Логотип " + technology);
                }
            }
            w.groupText("services-page", "outstaff", "title", "Услуги ITWEBS — полный цикл поддержки");

            w.text("services-page-process", "title", "Понятный процесс работы над вашим проектом");
            String[] stepTitles = {
                    "Стратегическая сессия по вашим задачам", "Детальное проектирование структуры",
                    "Согласование стоимости и сроков", "Юридический договор", "Разработка ТЗ",
                    "Проектирование дизайна", "Создание архитектуры", "Разработка",
                    "Многоуровневое тестирование", "Приёмочные испытания с вами",
                    "Регистрация и настройка аккаунтов", "Публикация приложения", "Пост-релизная поддержка 24/7"
            };
            String[] stepDescriptions = {
                    "Проводим глубокий анализ вашего бизнеса, чтобы точно определить цели и функциональные требования будущего продукта.",
                    "Создаём исчерпывающее описание всех страниц, ролей и сценариев использования — вы видите продукт ещё до старта разработки.",
                    "Фиксируем бюджет, поэтапный план и график оплаты — вы полностью контролируете процесс.",
                    "Оформляем договор, защищающий ваши интересы, с чёткими обязательствами и конфиденциальностью.",
                    "Готовим детализированное техническое задание, исключающее разночтения и гарантирующее точную реализацию.",
                    "Разрабатываем уникальный UI/UX, ориентированный на вашу аудиторию, с учётом современных трендов и гайдлайнов платформ.",
                    "Проектируем и реализуем надёжный бэкенд, обеспечивающий быстродействие, безопасность и масштабирование под ваш рост.",
                    "Разрабатываем клиентскую часть, которая будет радовать пользователей скоростью, удобством и безупречной работой.",
                    "Проводим всестороннее тестирование на реальных устройствах и эмуляторах, чтобы исключить любые ошибки.",
                    "Передаём вам продукт для финальной проверки, оперативно вносим правки по вашим замечаниям — вы получаете именно то, что хотели.",
                    "Помогаем открыть или настроить профиль разработчика, генерируем необходимые цифровые ключи и сертификаты для релиза.",
                    "Публикуем приложение в магазине и следим, чтобы оно успешно прошло проверку безопасности и вышло в свет.",
                    "Обеспечиваем круглосуточное сопровождение, консультации и оперативное решение любых технических вопросов."
            };
            for (int i = 0; i < stepTitles.length; i++) {
                String group = "step-" + (i + 1);
                w.groupText("services-page-process-steps", group, "number", Integer.toString(i + 1));
                w.groupText("services-page-process-steps", group, "title", stepTitles[i]);
                w.groupText("services-page-process-steps", group, "description", stepDescriptions[i]);
            }

            w.text("services-page-tasks", "eyebrow", "Реализуем отдельные функции или создадим приложение с нуля");
            w.text("services-page-tasks", "title", "Какие задачи мы решаем");
            String[] tasks = {
                    "Увеличение продаж и среднего чека", "Круглосуточный канал продаж",
                    "Рост лояльности через персонализацию", "Мгновенные push-уведомления",
                    "Интеграция с CRM и ERP", "Оплата в один клик",
                    "Конкурентное преимущество на рынке", "Сбор поведенческой аналитики",
                    "Повышение узнаваемости бренда", "Автоматизация сервисного обслуживания"
            };
            String[] taskDescriptions = {
                    "Персональные push-уведомления о скидках, push-напоминания о брошенной корзине. Продажи 24/7",
                    "Приложение работает как витрина в любое время. Клиент совершает покупки в удобный момент.",
                    "История заказов, персональные рекомендации, программа лояльности. Индивидуальный подход к каждому.",
                    "Высокая открываемость в сравнении с SMS и email. Быстрый возврат пользователя в приложение.",
                    "Единая экосистема: заказ из приложения сразу попадает в работу. Прозрачность выполнения.",
                    "Единая экосистема: заказ из приложения сразу попадает в работу. Прозрачность выполнения.",
                    "Единая экосистема: заказ из приложения сразу попадает в работу. Прозрачность выполнения.",
                    "Единая экосистема: заказ из приложения сразу попадает в работу. Прозрачность выполнения.",
                    "Единая экосистема: заказ из приложения сразу попадает в работу. Прозрачность выполнения.",
                    "Единая экосистема: заказ из приложения сразу попадает в работу. Прозрачность выполнения."
            };
            for (int i = 0; i < tasks.length; i++) {
                String group = "task-" + (i + 1);
                w.groupText("services-page-tasks", group, "label", "Решение " + (i + 1));
                w.groupText("services-page-tasks", group, "title", tasks[i]);
                w.groupText("services-page-tasks", group, "description", taskDescriptions[i]);
            }

            w.text("services-page-audience", "eyebrow", "Не предлагаем универсальных шаблонов — мы глубоко погружаемся в специфику вашей ниши и создаём эффективные Android-приложения под конкретные цели, будь то локальный бизнес, федеральная сеть или технологичный стартап");
            w.text("services-page-audience", "title", "Кому подходит услуга");
            String[] audiences = {"Малый бизнес", "Средний бизнес", "Стартапы"};
            String[] audienceDescriptions = {
                    "Помогаем внедрять современные IT-решения с заботой о каждом клиенте: быстрая разработка сайтов, мобильных приложений и CRM для роста локального присутствия и автоматизации.",
                    "Разрабатываем масштабируемые платформы и инструменты управления: от комплексной автоматизации до выхода на федеральный уровень с надежной IT-инфраструктурой.",
                    "Становимся технологическим партнером: от MVP до масштабирования. Быстрые итерации, экспертиза, помощь в привлечении инвестиций через качественный продукт."
            };
            for (int i = 0; i < audiences.length; i++) {
                String group = "audience-" + (i + 1);
                w.groupText("services-page-audiences", group, "title", audiences[i]);
                w.groupText("services-page-audiences", group, "description", audienceDescriptions[i]);
            }

            w.text("services-page-cooperation", "title", "Модели сотрудничества");
            String[] modelNames = {"Time and Material", "Fixed Price", "Retainer"};
            String[] modelDescriptions = {
                    "Оплачивается фактически затраченное время специалистов. Подходит сложным проектам с высоким уровнем неопределенности и потребностью гибко менять процесс разработки.",
                    "Оплачивается фиксированная стоимость всего проекта. Подходит проектам с четким пониманием результата, исчерпывающим техническим заданием, строгими сроками и бюджетом.",
                    "Оплачивается согласованное время работы специалистов в месяц. Подходит проектам с регулярным потоком задач на сопровождение, техническую поддержку, обновления и контроль качества."
            };
            for (int i = 0; i < modelNames.length; i++) {
                String group = "model-" + (i + 1);
                w.groupText("services-page-cooperation-models", group, "title", modelNames[i]);
                w.groupText("services-page-cooperation-models", group, "description", modelDescriptions[i]);
                w.groupText("services-page-cooperation-models", group, "price", "от 2.000 ₽ час");
            }

            w.text("services-page-projects", "eyebrow", "За 6 лет мы создали более 300 проектов. Здесь мы собрали самые яркие кейсы, о которых можем смело рассказывать!");
            w.text("services-page-projects", "title", "Проекты мобильных приложений для Android");
            Project[] projects = {
                    new Project("Bioflora — платформа микробиомной диагностики кожи", "Web разработка", "Личный кабинет с загрузкой анализов, визуализацией показателей и персональными рекомендациями по уходу.", "/cases/skin-insights"),
                    new Project("Приложение VPN с подключением в одно касание", "Mobile разработка", "Выбор страны, статистика трафика и автоподключение к ближайшему серверу. iOS и Android на одной кодовой базе.", "/cases/vpn-app"),
                    new Project("Testing is easy — сервис лабораторной диагностики", "Web разработка", "Запись на анализы, расшифровка результатов врачами и напоминания о приёме. Интеграция с сетью лабораторий.", "/cases/lab-testing"),
                    new Project("Bruce — AI-ассистент для ежедневной рефлексии", "Mobile разработка", "Голосовой помощник, дневник и разбор привычек. Приложение держит контекст переписки и работает офлайн.", "/cases/ai-assistant"),
                    new Project("Focus — таймер концентрации и дыхательных практик", "Mobile разработка", "Сессии глубокой работы, статистика по неделям и синхронизация между устройствами через iCloud.", "/cases/focus-app"),
                    new Project("Mirrorly — терминал копитрейдинга", "Цифровая автоматизация", "Открытые позиции, история сделок и подписка на стратегии трейдеров в реальном времени по вебсокетам.", "/cases/trading-terminal")
            };
            for (int i = 0; i < projects.length; i++) {
                Project project = projects[i];
                String group = "project-" + (i + 1);
                w.groupText("project-cards", group, "title", project.title());
                w.groupText("project-cards", group, "category", project.category());
                w.groupText("project-cards", group, "description", project.description());
                w.groupLink("project-cards", group, "details", "Подробнее", project.href());
                w.updateDefaultImageAlt("project-cards", group, "image", "Изображение проекта " + (i + 1), project.title());
                // Existing shared project image slots are populated through media CRUD.
            }
            for (int i = 0; i < projects.length; i++)
                w.groupText("services-page-project-items", "project-" + (i + 1), "projectGroup", "project-" + (i + 1));
            w.link("services-page-projects", "all", "Смотреть все проекты", "/cases");

            w.text("services-page-seo", "title", "Цифровая экосистема и IT-интеграция для бизнеса");
            w.text("services-page-seo", "readMore", "Читать все");
            w.text("services-page-seo", "paragraph1", "Компания ITWEBS — надежный IT-интегратор и веб-студия полного цикла, предоставляющая комплексные IT-услуги для автоматизации и масштабирования бизнеса в Москве, регионах и на европейском рынке.");
            String[] seoTitles = {"Веб-разработка", "Мобильная разработка", "Автоматизация процессов", "SEO-продвижение", "IT-аутсорсинг и софт"};
            String[] seoDescriptions = {
                    "Создание сайтов под ключ, продающих лендингов, интернет-магазинов, корпоративных порталов на 1С-Битрикс, а также аудит, редизайн и перенос на новые CMS.",
                    "Создание нативных и кроссплатформенных приложений для iOS и Android на базе технологий Flutter и React Native.",
                    "Проектирование, разработка и внедрение CRM и ERP-систем, интеграция с сайтом и телефонией, синхронизация с 1С, настройка обмена данными.",
                    "Комплексный SEO-аудит сайтов, поисковая оптимизация и продвижение интернет-магазинов в топ поисковых систем.",
                    "Разработка программного обеспечения на заказ, создание единой IT-инфраструктуры и цифровых экосистем для стартапов и крупных предприятий."
            };
            for (int i = 0; i < seoTitles.length; i++) {
                String group = "topic-" + (i + 1);
                w.groupText("services-page-seo-topics", group, "title", seoTitles[i]);
                w.groupText("services-page-seo-topics", group, "description", seoDescriptions[i]);
            }
            w.text("services-page-seo", "paragraph2", "Проектируем отказоустойчивые цифровые решения для сложных веб-проектов любого масштаба. Ознакомиться с отзывами о компании ITWEBS можно независимых площадках.");

            w.marker("servicesPageSeedV2");
        };
    }

    private record Card(String title, String price, String description) { }
    private record Project(String title, String category, String description, String href) { }

    private static class Writer {
        private final ContentRepository repository;
        private final Set<String> existing = new HashSet<>();
        private final java.util.Map<String, ContentItem> items = new java.util.HashMap<>();
        private int nextOrder;

        Writer(ContentRepository repository) {
            this.repository = repository;
            for (ContentItem item : repository.findAll()) {
                existing.add(identity(item.getSectionName(), item.getGroupName(), item.getItemKey()));
                items.put(identity(item.getSectionName(), item.getGroupName(), item.getItemKey()), item);
                nextOrder = Math.max(nextOrder, item.getSortOrder() + 1);
            }
        }

        private String identity(String section, String group, String key) {
            return section + "\u0000" + (group == null ? "" : group) + "\u0000" + key;
        }

        void text(String section, String key, String value) { add(section, null, key, "TEXT", value, null); }
        void link(String section, String key, String value, String href) { add(section, null, key, "LINK", value, href); }
        void groupText(String section, String group, String key, String value) { add(section, group, key, "TEXT", value, null); }
        void groupLink(String section, String group, String key, String value, String href) { add(section, group, key, "LINK", value, href); }
        void image(String section, String group, String key, String alt) { add(section, group, key, "IMAGE", alt, null); }

        void updateDefaultImageAlt(String section, String group, String key, String defaultAlt, String alt) {
            ContentItem item = items.get(identity(section, group, key));
            if (item != null && defaultAlt.equals(item.getValue())) {
                item.setValue(alt);
                repository.save(item);
            }
        }

        private void add(String section, String group, String key, String type, String value, String href) {
            if (!existing.add(identity(section, group, key))) return;
            ContentItem item = new ContentItem();
            item.setSectionName(section);
            item.setGroupName(group);
            item.setItemKey(key);
            item.setType(type);
            item.setLabel(key);
            item.setValue(value);
            item.setHref(href);
            item.setSortOrder(nextOrder++);
            item.setPublished(true);
            items.put(identity(section, group, key), repository.save(item));
        }

        void marker(String key) {
            ContentItem item = new ContentItem();
            item.setSectionName("_system");
            item.setItemKey(key);
            item.setType("TEXT");
            item.setValue("complete");
            item.setPublished(false);
            item.setSortOrder(nextOrder);
            repository.save(item);
        }
    }
}
