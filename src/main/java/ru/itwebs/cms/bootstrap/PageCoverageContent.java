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
public class PageCoverageContent {
    @Bean
    @Order(6)
    CommandLineRunner seedPageCoverage(ContentRepository repository) {
        return args -> {
            if (repository.existsBySectionNameAndItemKeyAndGroupName("_system", "pageCoverageSeedV1", null)) return;
            Writer w = new Writer(repository);
            w.add("header", null, "navProduct", "LINK", "Продукт в меню", "Продукт", "/product");
            w.add("header", null, "max", "LINK", "MAX в шапке", "MAX", "https://max.ru/u/f9LHodD0cOLXec3LEs9xKO5fxRLYkKFGNLGNVvgAJkROaaO8WUcjT-JIpOw");
            w.add("header", null, "maxIcon", "IMAGE", "Иконка MAX", "Иконка MAX", null);
            w.add("header", null, "home", "LINK", "Логотип: ссылка на главную", "ITWEBS — на главную", "/");
            w.add("footer", null, "home", "LINK", "Логотип: ссылка на главную", "ITWEBS — на главную", "/");
            w.add("outstaff-page-meta", null, "title", "TEXT", "Заголовок вкладки / SEO title", "Аутстаффинг IT-специалистов — ITWEBS", null);
            w.add("outstaff-page-meta", null, "description", "TEXT", "SEO description", "Предоставляем разработчиков, тестировщиков, аналитиков и DevOps-инженеров для усиления вашей команды: подбор за считанные дни, прозрачные условия и отчетность.", null);
            w.add("outstaff-page", null, "floatingCta", "LINK", "Плавающая кнопка", "Обсудить услугу", "/#lead-form");
            w.add("product-page-meta", null, "title", "TEXT", "Заголовок вкладки / SEO title", "Продукт — low-code платформа ITWEBS — ITWEBS", null);
            w.add("product-page-meta", null, "description", "TEXT", "SEO description", "Готовая low-code платформа ITWEBS: сайт за один день без программистов, встроенная админка, домен и индексация в Яндекс и Google под ключ.", null);
            w.add("product-page", null, "floatingCta", "LINK", "Плавающая кнопка", "Обсудить проект", "/#lead-form");
            w.add("outstaff-page-contact-specialists", "option-1", "key", "TEXT", "Значение выбора специалиста", "ux-ui", null);
            w.add("outstaff-page-contact-specialists", "option-2", "key", "TEXT", "Значение выбора специалиста", "frontend", null);
            w.add("outstaff-page-contact-specialists", "option-3", "key", "TEXT", "Значение выбора специалиста", "backend", null);
            w.add("outstaff-page-contact-specialists", "option-4", "key", "TEXT", "Значение выбора специалиста", "devops", null);
            w.add("outstaff-page-contact-specialists", "option-5", "key", "TEXT", "Значение выбора специалиста", "qa", null);
            w.add("outstaff-page-contact-specialists", "option-6", "key", "TEXT", "Значение выбора специалиста", "analyst", null);
            w.add("outstaff-page-contact-grades", "junior", "key", "TEXT", "Значение грейда", "junior", null);
            w.add("outstaff-page-contact-grades", "middle", "key", "TEXT", "Значение грейда", "middle", null);
            w.add("outstaff-page-contact-grades", "senior", "key", "TEXT", "Значение грейда", "senior", null);
            w.add("outstaff-page-team-overrides", "member-6", "photoAlt", "TEXT", "Alt фотографии на Outstaff", "Алёша", null);
            w.add("contact", null, "consentPrefix", "TEXT", "Текст перед ссылкой на политику", "Я соглашаюсь с ", null);
            w.updateDefaultLink("header", null, "nav1", "Услуги", "#services", "/services");
            w.updateDefaultLink("header", null, "nav2", "Проекты", "#projects", "/cases");
            w.updateDefaultLink("header", null, "nav3", "Аутстаффинг", "#staffing", "/outstaff");
            w.updateDefaultLink("header", null, "nav4", "Блог", "#seo", "/blog");
            w.updateDefaultLink("header", null, "nav5", "Контакты", "#contact", "/contacts");
            w.updateDefaultLink("header", null, "telegram", "Telegram", "#contact", "https://t.me/itwebsru");
            w.updateDefaultLink("footer", null, "telegram", "Telegram", "#contact", "https://t.me/itwebsru");
            w.updateDefaultLink("footer", null, "whatsapp", "WhatsApp", "#contact", "https://wa.me/79151785639");
            w.updateDefaultLink("footer", null, "privacy", "Политика обработки персональных данных", "#privacy", "/policy");
            w.updateDefaultLink("contact", null, "privacy", "политикой обработки персональных данных", "#privacy", "/policy");
            w.updateDefaultLink("footer-navigation", "nav-2", "link", "Услуги", "#services", "/services");
            w.updateDefaultLink("footer-navigation", "nav-3", "link", "Проекты", "#projects", "/cases");
            w.updateDefaultLink("footer-navigation", "nav-4", "link", "Аутстаффинг", "#staffing", "/outstaff");
            w.updateDefaultLink("footer-navigation", "nav-5", "link", "Блог", "#seo", "/blog");
            w.updateDefaultLink("footer-navigation", "nav-6", "link", "Контакты", "#contact", "/contacts");
            w.updateDefaultLink("footer-services", "service-1", "link", "Web разработка", "#services", "/services?category=web#services-catalog");
            w.updateDefaultLink("footer-services", "service-2", "link", "Mobile разработка", "#services", "/services?category=mobile#services-catalog");
            w.updateDefaultLink("footer-services", "service-3", "link", "Интеграция и разработка CRM систем", "#services", "/services?category=crm#services-catalog");
            w.updateDefaultLink("footer-services", "service-4", "link", "Цифровая автоматизация", "#services", "/services?category=automation#services-catalog");
            w.updateDefaultLink("footer-services", "service-5", "link", "Услуги в области 1С интеграций", "#services", "/services?category=1c#services-catalog");
            w.updateDefaultLink("footer-services", "service-6", "link", "Аутстаффинг IT специалистов", "#services", "/outstaff");
            w.add("_system", null, "pageCoverageSeedV1", "TEXT", "Page coverage seed", "complete", null);
        };
    }

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
        void updateDefaultLink(String section, String group, String key, String defaultValue, String defaultHref, String href) {
            ContentItem item = items.get(identity(section, group, key));
            if (item != null && defaultValue.equals(item.getValue()) && defaultHref.equals(item.getHref())) {
                item.setHref(href);
                repository.save(item);
            }
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
