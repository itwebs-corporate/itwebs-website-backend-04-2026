package ru.itwebs.cms.bootstrap;

import ru.itwebs.cms.entity.ContentItem;
import ru.itwebs.cms.repository.ContentRepository;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.util.HashSet;
import java.util.Set;

@Configuration
public class InitialContent {
    @Bean
    CommandLineRunner seed(ContentRepository repository) {
        return args -> {
            if (repository.existsBySectionNameAndItemKeyAndGroupName("_system", "desktopSeedV2", null)) return;
            Writer w = new Writer(repository);
            w.text("header", "brand", "ITWEBS");
            w.image("header", null, "logo", "Логотип ITWEBS");
            w.text("meta", "title", "ITWEBS — WEB & Mobile разработка");
            w.link("header", "nav1", "Услуги", "#services");
            w.link("header", "nav2", "Проекты", "#projects");
            w.link("header", "nav3", "Аутстаффинг", "#staffing");
            w.link("header", "nav4", "Блог", "#seo");
            w.link("header", "nav5", "Контакты", "#contact");
            w.link("header", "email", "tech@itwebs.ru", "mailto:tech@itwebs.ru");
            w.link("header", "telegram", "Telegram", "#contact");
            w.link("header", "whatsapp", "WhatsApp", "#contact");
            w.image("header", null, "telegramIcon", "Иконка Telegram");
            w.image("header", null, "whatsappIcon", "Иконка WhatsApp");
            w.text("hero", "eyebrow", "# Код, который работает");
            w.text("hero", "title", "Создание WEB & Mobile приложений");
            w.text("hero", "description", "Приложения, которыми удобно пользоваться. Создаем сложные сервисы, выдерживаем пиковые нагрузки");
            w.link("hero", "presentation", "Скачать презентацию", "#contact");
            w.image("hero", null, "presentationIcon", "Иконка скачивания презентации");
            w.link("hero", "cta", "Обсудить проект", "#contact");
            w.link("hero", "floatingCta", "Обсудить проект", "#contact");
            w.text("brands", "eyebrow", "200+ сотрудничеств с компаниями по всему миру");
            w.text("brands", "title", "Нам доверяют ведущие бренды");
            String[] brandNames = {"Curtis Times Company", "Spotify", "Regento", "molfar", "Cisco", "nutritiendra", "vicso", "NVIDIA", "Curtis Times Company", "Spotify", "Regento", "molfar"};
            for (int i = 0; i < brandNames.length; i++) { String g = "brand-" + (i + 1); w.groupText("brands", g, "name", brandNames[i]); w.image("brands", g, "logo", brandNames[i]); }
            w.link("brands", "more", "+", "#brands");
            w.text("services", "title", "Наши услуги");
            String[] categories = {"Web разработка", "Mobile разработка", "Цифровая автоматизация", "Аутстаффинг IT специалистов", "Интеграция и разработка CRM систем", "Услуги в области 1С интеграций"};
            for (int i = 0; i < categories.length; i++) w.groupText("service-categories", "category-" + (i + 1), "name", categories[i]);
            String[] serviceTitles = {"Разработка мобильного приложения для Android", "Разработка мобильного приложения для iOS", "Разработка мобильного приложения (iOS + Android)", "Разработка PWA приложений", "Доработка и оптимизация работы мобильного приложения"};
            String[] serviceDescriptions = {"Android-приложения на Kotlin и Java. Оптимизируем для всех устройств и экономим бюджет за счёт продуманной архитектуры.", "iOS-приложения с нативной производительностью. Используем Swift и новейшие технологии Apple для безупречного пользовательского опыта.", "Разработка кроссплатформенного мобильного приложения", "Прогрессивные веб-приложения (PWA), которые увеличивают продажи. Работают офлайн, устанавливаются на экран смартфона и грузятся за секунды.", "Ускоряем загрузку, снижаем краши и повышаем рейтинг в магазинах."};
            for (int i = 0; i < serviceTitles.length; i++) { String g = "service-" + (i + 1); w.groupText("service-cards", g, "tag", "# Mobile разработка"); w.groupText("service-cards", g, "title", serviceTitles[i]); w.groupText("service-cards", g, "price", "от 400.000 ₽"); w.groupText("service-cards", g, "description", serviceDescriptions[i]); }
            w.link("services", "all", "Смотреть все услуги", "#services");
            w.text("projects", "eyebrow", "За 6 лет мы создали более 300 проектов. Здесь мы собрали самые яркие кейсы, о которых можем смело рассказывать!");
            w.text("projects", "title", "Проекты");
            for (int i = 1; i <= 6; i++) {
                String g = "project-" + i;
                w.image("project-cards", g, "image", "Изображение проекта " + i);
                w.groupLink("project-cards", g, "link", "Открыть проект", "#projects");
            }
            w.link("projects", "all", "Смотреть все проекты", "#projects");
            w.text("stats", "title", "Мы в цифрах");
            String[] statTexts = {"проектов для бизнеса и личных брендов", "областей профессиональной экспертизы", "разработчиков в рядах нашей компании", "лет средний опыт работы наших сотрудников"};
            for (int i = 0; i < statTexts.length; i++) { String g = "stat-" + (i + 1); w.groupText("stat-cards", g, "number", "0"); w.groupText("stat-cards", g, "description", statTexts[i]); }
            w.text("technologies", "eyebrow", "Укомплектуем команду под любые технические задачи");
            w.text("technologies", "title", "Технологии которые мы используем");
            w.text("technologies", "description", "Проектируем, разрабатываем и интегрируем высоконагруженные сервисы со сложным функционалом и бизнес-логикой.");
            String[] tabs = {"Бэкенд", "Фронтенд", "Mobile", "Инфраструктура", "Тестирование", "Аналитика"};
            for (int i = 0; i < tabs.length; i++) w.groupText("technology-tabs", "tab-" + (i + 1), "name", tabs[i]);
            String[] techNames = {"Java", "Kotlin", "Scala", "Python", "Spring", "PostgreSQL", "Spring Boot", "MongoDB", "NoSQL", "Elastic", "Kafka", "RabbitMQ", "GraphQL"};
            for (int i = 0; i < techNames.length; i++) { String g = "technology-" + (i + 1); w.groupText("technology-items", g, "name", techNames[i]); w.image("technology-items", g, "logo", techNames[i]); }
            w.text("staffing", "eyebrow", "Подключаем IT-специалистов к проекту в сжатые сроки");
            w.text("staffing", "title", "Предоставление разработчиков (аутстаффинг)");
            w.text("staffing", "description", "Предоставляем разработчиков, тестировщиков, аналитиков и инженеров по сопровождению инфраструктуры для усиления вашей команды.");
            String[] roles = {"Дизайнер пользовательского интерфейса (UX/UI)", "Клиентская разработка (Front-end)", "Серверная разработка (Back-end)", "Инженер по сопровождению инфраструктуры (DevOps)", "Инженер по сопровождению инфраструктуры (DevOps)", "Автоматизированное тестирование"};
            String[] stacks = {"CJM • Количественные/Качественные исследования • Прототипирование", "Vue.js • Nuxt.js • JavaScript • TypeScript • HTML • CSS", "Laravel • 1С-Битрикс • MySQL • PostgreSQL • REST API", "Docker • Kubernetes • Terraform • Jenkins", "Java • JUnit • REST API • Автоматизация тестирования", "JavaScript • PHP • Vue.js • Node.js"};
            for (int i = 0; i < roles.length; i++) {
                String g = "role-" + (i + 1);
                w.groupText("staffing-cards", g, "tag", i < 3 ? "# Middle" : "# DevOps");
                w.groupText("staffing-cards", g, "tag1", new String[]{"# Design", "# Front-end", "# Back-end", "# DevOps", "# DevOps", "# DevOps"}[i]);
                w.groupText("staffing-cards", g, "tag2", i < 3 ? "# Middle" : "# Руководитель направления");
                if (i == 0 || i == 2) w.groupText("staffing-cards", g, "tag3", i == 0 ? "# Seniour" : "# Team lead");
                w.groupText("staffing-cards", g, "title", roles[i]);
                w.groupText("staffing-cards", g, "stack", stacks[i]);
                w.groupText("staffing-cards", g, "price", "от 2.000 ₽/час");
                w.groupLink("staffing-cards", g, "link", "Подробнее", "#contact");
            }
            w.link("staffing", "all", "Смотреть все позиции", "#staffing");
            w.text("team", "eyebrow", "Наши специалисты для ваших проектов");
            w.text("team", "title", "Команда");
            String[] names = {"Дарина", "Евгений", "Светлана", "Дарина", "Станислав", "Алёна"};
            String[] positions = {"UI/UX Designer", "Руководитель направления", "Системный аналитик", "UI/UX Designer", "DevOps", "Разработчик"};
            for (int i = 0; i < names.length; i++) { String g = "member-" + (i + 1); w.groupText("team-members", g, "name", names[i]); w.groupText("team-members", g, "position", positions[i]); w.image("team-members", g, "photo", "Фото: " + names[i]); }
            w.text("contact", "eyebrow", "Расскажите, что необходимо сделать, — мы изучим детали и сразу свяжемся с вами");
            w.text("contact", "title", "Связаться с нами");
            w.text("contact", "name", "Ваше имя");
            w.text("contact", "phone", "Телефон для связи");
            w.text("contact", "email", "Почта");
            w.text("contact", "message", "Расскажите о вашей задаче");
            w.text("contact", "hint", "Например, какую основную проблему вам нужно решить, поделитесь целью, которую хотите достичь");
            w.text("contact", "method", "Как с вами лучше связаться");
            w.text("contact", "methodPhone", "перезвонить");
            w.text("contact", "methodEmail", "написать на почту");
            w.text("contact", "methodTelegram", "ответить в telegram");
            w.text("contact", "methodWhatsapp", "ответить в whatsapp");
            w.text("contact", "consent", "Я соглашаюсь с политикой обработки персональных данных");
            w.link("contact", "privacy", "политикой обработки персональных данных", "#privacy");
            w.text("contact", "submit", "Отправить запрос");
            w.text("contact", "sent", "Заявка отправлена. Мы свяжемся с вами.");
            w.text("seo", "eyebrow", "Для поискового интента");
            w.text("seo", "title", "Разработка сайтов и мобильных приложений в Москве под ключ: от идеи до продаж");
            w.text("seo", "paragraph1", "Студия ITWEBS создает кастомные IT-решения под ключ для малого, среднего и крупного бизнеса. Мы предлагаем индивидуальную разработку продающих интернет-магазинов, современных корпоративных сайтов, сложных веб-порталов и адаптивных лендингов без использования шаблонов.");
            w.text("seo", "paragraph2", "Наша команда осуществляет проектирование и запуск нативных и кроссплатформенных мобильных приложений для iOS и Android, а также настраивает интеграцию с 1С, CRM- и ERP-системами для автоматизации бизнес-процессов.");
            w.text("seo", "subtitle", "Комплексные IT-услуги для развития вашего бизнеса");
            w.text("seo", "services", "Создание сайтов любой сложности • Разработка мобильных приложений • Интеграция CRM и ERP • Аутстафф разработчиков");
            String[] seoServices = {"Создание сайтов любой сложности", "Разработка мобильных приложений", "Интеграция CRM и ERP", "Аутстафф разработчиков"};
            for (int i = 0; i < seoServices.length; i++) w.groupText("seo-services", "service-" + (i + 1), "name", seoServices[i]);
            w.text("footer", "description", "Создание, поддержка и развитие WEB & Mobile приложений");
            w.image("footer", null, "logo", "Логотип ITWEBS");
            w.link("footer", "sales", "sales@itwebs.ru", "mailto:sales@itwebs.ru");
            w.link("footer", "tech", "tech@itwebs.ru", "mailto:tech@itwebs.ru");
            w.link("footer", "phone", "+7 (915) 178-56-39", "tel:+79151785639");
            w.link("footer", "telegram", "Telegram", "#contact");
            w.link("footer", "whatsapp", "WhatsApp", "#contact");
            w.image("footer", null, "telegramIcon", "Иконка Telegram");
            w.image("footer", null, "whatsappIcon", "Иконка WhatsApp");
            w.text("footer", "company", "ООО «ПрофИнфоТех-С»");
            w.text("footer", "inn", "ИНН/КПП 9705250373 / 770501001");
            w.text("footer", "ogrn", "ОГРН 1257700507800");
            w.text("footer", "address", "119017, г. Москва, ул. Пятницкая, д. 37, пом. 1/1");
            w.text("footer", "navTitle", "Навигация");
            w.text("footer", "servicesTitle", "Услуги");
            w.text("footer", "regionsTitle", "Регионы");
            w.text("footer", "regions", "Москва • Красноярск • Екатеринбург");
            String[] footerNavNames = {"Главная", "Услуги", "Проекты", "Аутстаффинг", "Блог", "Контакты"};
            String[] footerNavHrefs = {"/", "#services", "#projects", "#staffing", "#seo", "#contact"};
            for (int i = 0; i < footerNavNames.length; i++)
                w.groupLink("footer-navigation", "nav-" + (i + 1), "link", footerNavNames[i], footerNavHrefs[i]);
            String[] footerServiceNames = {"Web разработка", "Mobile разработка", "Интеграция и разработка CRM систем", "Цифровая автоматизация", "Услуги в области 1С интеграций", "Аутстаффинг IT специалистов"};
            for (int i = 0; i < footerServiceNames.length; i++)
                w.groupLink("footer-services", "service-" + (i + 1), "link", footerServiceNames[i], "#services");
            String[] footerRegions = {"Москва", "Красноярск", "Екатеринбург"};
            for (int i = 0; i < footerRegions.length; i++)
                w.groupLink("footer-regions", "region-" + (i + 1), "link", footerRegions[i], "#contact");
            w.text("footer", "copyright", "Copyright ©2026 ITWEBS");
            w.link("footer", "privacy", "Политика обработки персональных данных", "#privacy");
            w.marker("desktopSeedV2");
        };
    }

    private static class Writer {
        private final ContentRepository repository;
        private final Set<String> existing = new HashSet<>();
        private final boolean fresh;
        private int nextNewOrder;
        private int order;
        Writer(ContentRepository repository) {
            this.repository = repository;
            var items = repository.findAll();
            fresh = items.isEmpty();
            for (ContentItem item : items) {
                existing.add(identity(item.getSectionName(), item.getGroupName(), item.getItemKey()));
                nextNewOrder = Math.max(nextNewOrder, item.getSortOrder() + 1);
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
        void marker(String key) {
            ContentItem item = new ContentItem();
            item.setSectionName("_system"); item.setItemKey(key); item.setType("TEXT");
            item.setValue("complete"); item.setPublished(false); item.setSortOrder(nextNewOrder);
            repository.save(item);
        }
        void add(String section, String group, String key, String type, String value, String href) {
            int position = fresh ? order++ : nextNewOrder++;
            if (!existing.add(identity(section, group, key))) return;
            ContentItem item = new ContentItem();
            item.setSectionName(section); item.setGroupName(group); item.setItemKey(key);
            item.setType(type); item.setLabel(key); item.setValue(value); item.setHref(href);
            item.setSortOrder(position); item.setPublished(true); repository.save(item);
        }
    }
}
