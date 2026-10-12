package ru.itwebs.cms.bootstrap;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import ru.itwebs.cms.entity.ContentItem;
import ru.itwebs.cms.repository.ContentRepository;
import java.util.HashSet;
import java.util.Set;

@Configuration
public class ProductPageContent {
    @Bean
    @Order(5)
    CommandLineRunner seedProductPage(ContentRepository repository) {
        return args -> {
            if (repository.existsBySectionNameAndItemKeyAndGroupName("_system", "productPageSeedV1", null)) return;
            Writer w = new Writer(repository);
            w.add("product-page", null, "eyebrow", "TEXT", "Надзаголовок", "# Наш продукт", null);
            w.add("product-page", null, "title", "TEXT", "Заголовок", "Запустите сайт за 1 день без программистов и лишних затрат", null);
            w.add("product-page", null, "description", "TEXT", "Описание", "Мы создали готовую low-code платформу от ITWEBS, которая позволяет вашему бизнесу мгновенно получить современный сайт, полностью готовый к продвижению и привлечению клиентов.", null);
            w.add("product-page-advantages", null, "eyebrow", "TEXT", "Надзаголовок", "Скорость шаблона, надежность кастомной разработки", null);
            w.add("product-page-advantages", null, "title", "TEXT", "Заголовок", "Почему это выгодно вашему бизнесу (Преимущества)", null);
            w.add("product-page-advantages", null, "description", "TEXT", "Описание", "Вместо месяцев ожидания и огромных бюджетов вы получаете протестированное решение, развернутое на технологиях нашей компании.", null);
            w.add("product-page-advantage-items", "item-1", "number", "TEXT", "Карточка 1: number", "1", null);
            w.add("product-page-advantage-items", "item-1", "title", "TEXT", "Карточка 1: title", "Старт за 24 часа", null);
            w.add("product-page-advantage-items", "item-1", "description", "TEXT", "Карточка 1: description", "Сайт собирается из готовых, заранее отлаженных нами UI-компонентов.", null);
            w.add("product-page-advantage-items", "item-2", "number", "TEXT", "Карточка 2: number", "2", null);
            w.add("product-page-advantage-items", "item-2", "title", "TEXT", "Карточка 2: title", "Экономия на запуске", null);
            w.add("product-page-advantage-items", "item-2", "description", "TEXT", "Карточка 2: description", "Оплата по прозрачной модели подписки - без затрат на хостинг и администрирование.", null);
            w.add("product-page-advantage-items", "item-3", "number", "TEXT", "Карточка 3: number", "3", null);
            w.add("product-page-advantage-items", "item-3", "title", "TEXT", "Карточка 3: title", "Удобное управление", null);
            w.add("product-page-advantage-items", "item-3", "description", "TEXT", "Карточка 3: description", "Простая встроенная админка позволяет вам менять любые тексты и SEO-параметры без привлечения разработчиков.", null);
            w.add("product-page-advantage-items", "item-4", "number", "TEXT", "Карточка 4: number", "4", null);
            w.add("product-page-advantage-items", "item-4", "title", "TEXT", "Карточка 4: title", "Все включено", null);
            w.add("product-page-advantage-items", "item-4", "description", "TEXT", "Карточка 4: description", "Мы сами зарегистрируем домен на ваше имя и автоматически настроим индексацию в Яндекс и Google.", null);
            w.add("product-page-control", null, "eyebrow", "TEXT", "Надзаголовок", "Как это устроено", null);
            w.add("product-page-control", null, "title", "TEXT", "Заголовок", "Вы управляете контентом - мы отвечаем за код", null);
            w.add("product-page-control", null, "description", "TEXT", "Описание", "Мы избавили вас от необходимости нанимать штатных IT-специалистов. Все сложные технические задачи мы берем на себя.", null);
            w.add("product-page-control-items", "item-1", "number", "TEXT", "Карточка 1: number", "1", null);
            w.add("product-page-control-items", "item-1", "title", "TEXT", "Карточка 1: title", "Ваша зона контроля", null);
            w.add("product-page-control-items", "item-1", "description", "TEXT", "Карточка 1: description", "Вы легко и в любой момент меняете тексты, контакты, отзывы и изображения через админ-панель.", null);
            w.add("product-page-control-items", "item-2", "number", "TEXT", "Карточка 2: number", "2", null);
            w.add("product-page-control-items", "item-2", "title", "TEXT", "Карточка 2: title", "Наша поддержка", null);
            w.add("product-page-control-items", "item-2", "description", "TEXT", "Карточка 2: description", "Если нужно переставить блоки, изменить стили или обновить структуру — наши разработчики сделают это моментально по вашему запросу.", null);
            w.add("product-page-control-items", "item-3", "number", "TEXT", "Карточка 3: number", "3", null);
            w.add("product-page-control-items", "item-3", "title", "TEXT", "Карточка 3: title", "Никакого визуального хаоса", null);
            w.add("product-page-control-items", "item-3", "description", "TEXT", "Карточка 3: description", "У клиента нет доступа к коду и стилистике, что гарантирует — дизайн вашего сайта всегда останется чистым, ровным и профессиональным.", null);
            w.add("product-page-use-cases", null, "eyebrow", "TEXT", "Надзаголовок", "Кому и для каких задач это подойдет", null);
            w.add("product-page-use-cases", null, "title", "TEXT", "Заголовок", "Идеальный инструмент для быстрого старта", null);
            w.add("product-page-use-cases", null, "description", "TEXT", "Описание", "Платформа создана для проектов, где важен быстрый запуск и моментальная отдача от маркетинга.", null);
            w.add("product-page-use-case-items", "use-case-1", "tag", "TEXT", "Пример 1: tag", "# Корпоративные сайты", null);
            w.add("product-page-use-case-items", "use-case-1", "title", "TEXT", "Пример 1: title", "Корпоративные сайты", null);
            w.add("product-page-use-case-items", "use-case-1", "description", "TEXT", "Пример 1: description", "Огромная кнопка включения в центре экрана, меняющая цвет в зависимости от статуса (активно/выключено). Никакого визуального шума.", null);
            w.add("product-page-use-case-items", "use-case-1", "projectGroup", "TEXT", "Пример 1: projectGroup", "project-12", null);
            w.add("product-page-use-case-items", "use-case-1", "cta", "LINK", "cta", "Попробовать продукт", "/product#lead-form");
            w.add("product-page-use-case-items", "use-case-1", "imageMobile", "IMAGE", "Мобильное изображение примера 1", "Мобильное приложение Learn English By Listening", null);
            w.add("product-page-use-case-items", "use-case-2", "tag", "TEXT", "Пример 2: tag", "# Акционные лендинги", null);
            w.add("product-page-use-case-items", "use-case-2", "title", "TEXT", "Пример 2: title", "Акционные лендинги", null);
            w.add("product-page-use-case-items", "use-case-2", "description", "TEXT", "Пример 2: description", "Быстрые посадочные страницы под запуск контекстной или таргетированной рекламы", null);
            w.add("product-page-use-case-items", "use-case-2", "projectGroup", "TEXT", "Пример 2: projectGroup", "project-5", null);
            w.add("product-page-use-case-items", "use-case-2", "cta", "LINK", "cta", "Попробовать продукт", "/product#lead-form");
            w.add("product-page-use-case-items", "use-case-2", "imageMobile", "IMAGE", "Мобильное изображение примера 2", "Focus — таймер концентрации и дыхательных практик", null);
            w.add("product-page-use-case-items", "use-case-3", "tag", "TEXT", "Пример 3: tag", "# Проекты на вырост", null);
            w.add("product-page-use-case-items", "use-case-3", "title", "TEXT", "Пример 3: title", "Проекты на вырост", null);
            w.add("product-page-use-case-items", "use-case-3", "description", "TEXT", "Пример 3: description", "Начните с визитки, а при необходимости мы бесшовно подключим модули интернет-магазина, корзины, личных кабинетов или онлайн-оплаты.", null);
            w.add("product-page-use-case-items", "use-case-3", "projectGroup", "TEXT", "Пример 3: projectGroup", "project-8", null);
            w.add("product-page-use-case-items", "use-case-3", "cta", "LINK", "cta", "Попробовать продукт", "/product#lead-form");
            w.add("product-page-use-case-items", "use-case-3", "projectTitle", "TEXT", "Название проекта на странице Product", "Vesna Hypnotherapy", null);
            w.add("product-page-use-case-items", "use-case-3", "projectDescription", "TEXT", "Описание проекта на странице Product", "Начните с визитки, а при необходимости мы бесшовно подключим модули интернет-магазина, корзины, личных кабинетов или онлайн-оплаты.", null);
            w.add("project-cards", "project-12", "title", "TEXT", "Название проекта Learn English", "Мобильное приложение Learn English By Listening", null);
            w.add("project-cards", "project-12", "category", "TEXT", "Категория проекта Learn English", "Mobile разработка", null);
            w.add("project-cards", "project-12", "description", "TEXT", "Описание проекта Learn English", "Мобильное приложение для изучения английского через игры с видео, подкастами и мгновенным переводом субтитров.", null);
            w.add("project-cards", "project-12", "details", "LINK", "Ссылка на проект Learn English", "Подробнее", "/cases/learn-english");
            w.add("project-cards", "project-12", "image", "IMAGE", "Десктопное изображение Learn English", "Мобильное приложение Learn English By Listening", null);
            w.add("product-page-stages", null, "eyebrow", "TEXT", "Надзаголовок", "Как начать", null);
            w.add("product-page-stages", null, "title", "TEXT", "Заголовок", "Готовы запустить проект?", null);
            w.add("product-page-stages", null, "description", "TEXT", "Описание", "Оставьте заявку, и мы поможем подобрать идеальную структуру под ваши задачи.", null);
            w.add("product-page-stage-items", "item-1", "label", "TEXT", "Карточка 1: label", "# Этап 1", null);
            w.add("product-page-stage-items", "item-1", "title", "TEXT", "Карточка 1: title", "Бриф", null);
            w.add("product-page-stage-items", "item-1", "description", "TEXT", "Карточка 1: description", "Рассказываете, какие страницы, цвета и блоки вам необходимы.", null);
            w.add("product-page-stage-items", "item-2", "label", "TEXT", "Карточка 2: label", "# Этап 2", null);
            w.add("product-page-stage-items", "item-2", "title", "TEXT", "Карточка 2: title", "Договор", null);
            w.add("product-page-stage-items", "item-2", "description", "TEXT", "Карточка 2: description", "Мы четко фиксируем финальный вид сайта и закрепляем условия.", null);
            w.add("product-page-stage-items", "item-3", "label", "TEXT", "Карточка 3: label", "# Этап 3", null);
            w.add("product-page-stage-items", "item-3", "title", "TEXT", "Карточка 3: title", "Результат", null);
            w.add("product-page-stage-items", "item-3", "description", "TEXT", "Карточка 3: description", "Вы получаете готовое работающее решение в ваших корпоративных цветах.", null);
            w.add("product-page-audience", null, "eyebrow", "TEXT", "Надзаголовок", "Не навязываем сложные и дорогие процессы разработки там, где нужен быстрый старт - мы создали гибкую low-code платформу под конкретные задачи вашего бизнеса, будь то запуск рекламы на услугу, автоматизация локальной точки или проверка гипотезы.", null);
            w.add("product-page-audience", null, "title", "TEXT", "Заголовок", "Кому подходит продукт", null);
            w.add("product-page-audience-items", "item-1", "title", "TEXT", "Карточка 1: title", "Малый бизнес", null);
            w.add("product-page-audience-items", "item-1", "description", "TEXT", "Карточка 1: description", "Помогаем запустить полноценный сайт за 24 часа без затрат на программистов. Идеально для локальных компаний, самозанятых и сферы услуг.", null);
            w.add("product-page-audience-items", "item-2", "title", "TEXT", "Карточка 2: title", "Маркетологи и SEO", null);
            w.add("product-page-audience-items", "item-2", "description", "TEXT", "Карточка 2: description", "Предоставляем чистый код, готовую авто-индексацию и гибкое управление контентом. Создавайте посадочные страницы под акции и тестируйте поисковые гипотезы.", null);
            w.add("product-page-audience-items", "item-3", "title", "TEXT", "Карточка 3: title", "Стартапы и MVP", null);
            w.add("product-page-audience-items", "item-3", "description", "TEXT", "Карточка 3: description", "Позволяем быстро и с минимальным бюджетом проверить жизнеспособность бизнес-идеи в сети. Запускайте презентацию продукта на готовых UI-компонентах.", null);
            w.add("product-page-contact", null, "hint", "TEXT", "hint", "например, какую основную проблему вам нужно решить, поделитесь целью, которую хотите достичь", null);
            w.add("product-page-contact", null, "services", "LINK", "Ссылка на все услуги", "Смотреть все услуги", "/services");
            w.add("product-page-seo", null, "eyebrow", "TEXT", "Надзаголовок", "Для поискового интента", null);
            w.add("product-page-demo", null, "adminTitle", "TEXT", "Демонстрация ProBuilder: adminTitle", "ProBuilder Admin", null);
            w.add("product-page-demo", null, "analytics", "TEXT", "Демонстрация ProBuilder: analytics", "Analytics", null);
            w.add("product-page-demo", null, "team", "TEXT", "Демонстрация ProBuilder: team", "Team", null);
            w.add("product-page-demo", null, "billing", "TEXT", "Демонстрация ProBuilder: billing", "billing", null);
            w.add("product-page-demo", null, "publish", "TEXT", "Демонстрация ProBuilder: publish", "Publish", null);
            w.add("product-page-demo", null, "settingsTitle", "TEXT", "Демонстрация ProBuilder: settingsTitle", "SEO & Site Settings", null);
            w.add("product-page-demo", null, "settingsDescription", "TEXT", "Демонстрация ProBuilder: settingsDescription", "Manage your domain, search presence, and global site configurations.", null);
            w.add("product-page-demo", null, "domainTitle", "TEXT", "Демонстрация ProBuilder: domainTitle", "Domain Name", null);
            w.add("product-page-demo", null, "domainDescription", "TEXT", "Демонстрация ProBuilder: domainDescription", "Connect a custom domain to your project to make it accessible to the public.", null);
            w.add("product-page-demo", null, "domain", "TEXT", "Демонстрация ProBuilder: domain", "www.yourdomain.com", null);
            w.add("product-page-demo", null, "protocol", "TEXT", "Демонстрация ProBuilder: protocol", "https://", null);
            w.add("product-page-demo", null, "connect", "TEXT", "Демонстрация ProBuilder: connect", "Connect", null);
            w.add("product-page-demo", null, "dnsHint", "TEXT", "Демонстрация ProBuilder: dnsHint", "DNS propagation may take up to 48 hours after connection.", null);
            w.add("product-page-demo", null, "indexingTitle", "TEXT", "Демонстрация ProBuilder: indexingTitle", "Search Engine Indexing", null);
            w.add("product-page-demo", null, "indexingDescription", "TEXT", "Демонстрация ProBuilder: indexingDescription", "Control how search engines crawl and index your live site.", null);
            w.add("product-page-demo", null, "googleTitle", "TEXT", "Демонстрация ProBuilder: googleTitle", "Google Search Console", null);
            w.add("product-page-demo", null, "googleDescription", "TEXT", "Демонстрация ProBuilder: googleDescription", "Enable sitemap submission and indexing.", null);
            w.add("product-page-demo", null, "yandexTitle", "TEXT", "Демонстрация ProBuilder: yandexTitle", "Yandex Webmaster", null);
            w.add("product-page-demo", null, "yandexDescription", "TEXT", "Демонстрация ProBuilder: yandexDescription", "Enable analytics and indexing for Yandex.", null);
            w.add("product-page-demo", null, "subscriptionTitle", "TEXT", "Демонстрация ProBuilder: subscriptionTitle", "SUBSCRIPTION", null);
            w.add("product-page-demo", null, "plan", "TEXT", "Демонстрация ProBuilder: plan", "Pro Plan", null);
            w.add("product-page-demo", null, "status", "TEXT", "Демонстрация ProBuilder: status", "Active", null);
            w.add("product-page-demo", null, "price", "TEXT", "Демонстрация ProBuilder: price", "$29.00 / month", null);
            w.add("product-page-demo", null, "nextBillingLabel", "TEXT", "Демонстрация ProBuilder: nextBillingLabel", "Next Billing Date", null);
            w.add("product-page-demo", null, "nextBillingValue", "TEXT", "Демонстрация ProBuilder: nextBillingValue", "Oct 15, 2024", null);
            w.add("product-page-demo", null, "paymentLabel", "TEXT", "Демонстрация ProBuilder: paymentLabel", "Payment Method", null);
            w.add("product-page-demo", null, "paymentValue", "TEXT", "Демонстрация ProBuilder: paymentValue", "Visa •••• 4242", null);
            w.add("product-page-demo", null, "manageBilling", "TEXT", "Демонстрация ProBuilder: manageBilling", "Manage Billing", null);
            w.add("product-page-demo", null, "contactsTitle", "TEXT", "Демонстрация ProBuilder: contactsTitle", "GLOBAL CONTACT DETAILS", null);
            w.add("product-page-demo", null, "contactsDescription", "TEXT", "Демонстрация ProBuilder: contactsDescription", "Used for site-wide forms and standard metadata.", null);
            w.add("product-page-demo", null, "emailLabel", "TEXT", "Демонстрация ProBuilder: emailLabel", "Global Email", null);
            w.add("product-page-demo", null, "emailValue", "TEXT", "Демонстрация ProBuilder: emailValue", "hello@yourdomain.com", null);
            w.add("product-page-demo", null, "phoneLabel", "TEXT", "Демонстрация ProBuilder: phoneLabel", "Global Phone", null);
            w.add("product-page-demo", null, "phoneValue", "TEXT", "Демонстрация ProBuilder: phoneValue", "+1 (555) 000-0000", null);
            w.add("product-page-demo", null, "saveDetails", "TEXT", "Демонстрация ProBuilder: saveDetails", "Save Details", null);
            w.add("product-page-demo", null, "library", "TEXT", "Демонстрация ProBuilder: library", "Library", null);
            w.add("product-page-demo", null, "editor", "TEXT", "Демонстрация ProBuilder: editor", "Editor", null);
            w.add("product-page-demo", null, "logo", "TEXT", "Демонстрация ProBuilder: logo", "P", null);
            w.add("product-page-demo", null, "brand", "TEXT", "Демонстрация ProBuilder: brand", "ProBuilder", null);
            w.add("product-page-demo", null, "version", "TEXT", "Демонстрация ProBuilder: version", "V2.4.0-Stable", null);
            w.add("product-page-demo", null, "newProject", "TEXT", "Демонстрация ProBuilder: newProject", "New Project", null);
            w.add("product-page-demo", null, "dashboard", "TEXT", "Демонстрация ProBuilder: dashboard", "Dashboard", null);
            w.add("product-page-demo", null, "seo", "TEXT", "Демонстрация ProBuilder: seo", "SEO", null);
            w.add("product-page-demo", null, "settings", "TEXT", "Демонстрация ProBuilder: settings", "Settings", null);
            w.add("product-page-demo", null, "help", "TEXT", "Демонстрация ProBuilder: help", "Help Center", null);
            w.add("product-page-demo", null, "previewAlt", "TEXT", "Демонстрация ProBuilder: previewAlt", "Админ-панель ProBuilder: домен, индексация, подписка и контакты", null);
            w.add("product-page-demo", null, "menuAlt", "TEXT", "Демонстрация ProBuilder: menuAlt", "Меню админки ProBuilder: разделы Dashboard, Editor, Library, SEO и Settings", null);
            w.add("_system", null, "productPageSeedV1", "TEXT", "Product seed", "complete", null);
        };
    }

    private static class Writer {
        private final ContentRepository repository;
        private final Set<String> existing = new HashSet<>();
        private int nextOrder;
        Writer(ContentRepository repository) {
            this.repository = repository;
            for (ContentItem item : repository.findAll()) {
                existing.add(identity(item.getSectionName(), item.getGroupName(), item.getItemKey()));
                nextOrder = Math.max(nextOrder, item.getSortOrder() + 1);
            }
        }
        private String identity(String section, String group, String key) {
            return section + "\u0000" + (group == null ? "" : group) + "\u0000" + key;
        }
        void add(String section, String group, String key, String type, String label, String value, String href) {
            if (!existing.add(identity(section, group, key))) return;
            ContentItem item = new ContentItem();
            item.setSectionName(section);
            item.setGroupName(group);
            item.setItemKey(key);
            item.setType(type);
            item.setLabel(label);
            item.setValue(value);
            item.setHref(href);
            item.setSortOrder(nextOrder++);
            item.setPublished(!section.equals("_system"));
            repository.save(item);
        }
    }
}
