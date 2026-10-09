package ru.itwebs.cms.bootstrap;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import ru.itwebs.cms.entity.ContentItem;
import ru.itwebs.cms.repository.ContentRepository;

import java.util.HashSet;
import java.util.Set;

/** Seeds the cases index and its many-to-many filter membership. Project details and
 * image slots are kept in the shared project-cards section. */
@Configuration
public class CasesPageContent {
    @Bean
    @Order(3)
    CommandLineRunner seedCasesPage(ContentRepository repository) {
        return args -> {
            if (repository.existsBySectionNameAndItemKeyAndGroupName("_system", "casesPageSeedV1", null)) return;
            Writer w = new Writer(repository);

            w.text("cases-page", "eyebrow", "# Код, который работает");
            w.text("cases-page", "title", "Проекты которыми мы гордимся");
            w.text("cases-page", "description1", "За 6 лет мы создали более 300 проектов");
            w.text("cases-page", "description2", "Мы собрали самые яркие кейсы, о которых можем смело рассказывать!");

            String[] filters = {"Все", "Mobile", "Web", "CRM", "E-com", "AI"};
            String[] filterKeys = {"all", "mobile", "web", "crm", "ecom", "ai"};
            for (int i = 0; i < filters.length; i++) {
                String group = "filter-" + filterKeys[i];
                w.groupText("cases-page-filters", group, "key", filterKeys[i]);
                w.groupText("cases-page-filters", group, "label", filters[i]);
            }

            // The first six cards are shared with the Services page. Add the five
            // cases unique to this index to that same project collection.
            Project[] additionalProjects = {
                    new Project("Sentra — сайт платформы организационной памяти", "Web разработка", "Маркетинговый сайт B2B-продукта: интерактивная сфера знаний, блок инвесторов и форма раннего доступа.", "/cases/sentra"),
                    new Project("Vesna Hypnotherapy — сайт частной практики", "Web разработка", "Айдентика с градиентными переливами, лендинг услуг и онлайн-запись на сессии с оплатой.", "/cases/vesna"),
                    new Project("Bizwise — CRM-кабинет для малого бизнеса", "Интеграция и разработка CRM систем", "Карточки клиентов, списки задач и права команды. Интеграция телефонии, почты и платежей.", "/cases/bizwise"),
                    new Project("Bioflora — промо-кампания Test better. Treat better", "Web разработка", "Серия посадочных страниц о диагностике кожи: сторителлинг, каталог тестов и переход в личный кабинет.", "/cases/test-better"),
                    new Project("Petsy — соцсеть для владельцев питомцев", "Mobile разработка", "Профили животных, лента фотографий, поиск друзей поблизости и напоминания о прогулках.", "/cases/petsy")
            };
            for (int i = 0; i < additionalProjects.length; i++) {
                Project project = additionalProjects[i];
                String group = "project-" + (i + 7);
                w.groupText("project-cards", group, "title", project.title());
                w.groupText("project-cards", group, "category", project.category());
                w.groupText("project-cards", group, "description", project.description());
                w.groupLink("project-cards", group, "details", "Подробнее", project.href());
                w.image("project-cards", group, "image", project.title());
            }

            // Each case references one project record and can belong to more than
            // one filter. This matches the live page (e.g. AI and Web overlap).
            Case[] cases = {
                    new Case("project-1", "web", "ai"),
                    new Case("project-2", "mobile"),
                    new Case("project-7", "web", "ai"),
                    new Case("project-8", "web", "ecom"),
                    new Case("project-9", "web", "crm"),
                    new Case("project-4", "mobile", "ai"),
                    new Case("project-10", "web", "ecom"),
                    new Case("project-6", "web", "ai"),
                    new Case("project-11", "mobile"),
                    new Case("project-5", "mobile")
            };
            for (int i = 0; i < cases.length; i++) {
                Case item = cases[i];
                String group = "case-" + (i + 1);
                w.groupText("cases-page-items", group, "projectGroup", item.projectGroup());
                for (int j = 0; j < item.filters().length; j++)
                    w.groupText("cases-page-items", group, "filter" + (j + 1), item.filters()[j]);
            }
            w.link("cases-page", "cta", "Стать нашим клиентом", "/cases#lead-form");

            w.text("cases-page-seo", "title", "Кейсы ITWEBS: успешные проекты в сфере веб- и мобильной разработки");
            w.text("cases-page-seo", "paragraph1", "Мы гордимся созданием цифровых продуктов, которые решают реальные бизнес-задачи и приносят прибыль нашим клиентам. В портфолио студии ITWEBS представлены десятки успешно реализованных проектов: от продающих корпоративных сайтов до высоконагруженных мобильных приложений. Мы разрабатываем кастомные IT-решения под ключ, обеспечивая глубокую аналитику рынка, современный UI/UX дизайн и безупречную техническую реализацию для компаний из самых разных отраслей бизнеса.");
            w.text("cases-page-seo", "paragraph2", "Наша команда имеет богатый опыт запуска нативных и кроссплатформенных мобильных приложений для iOS и Android, а также сложных веб-порталов, CRM-систем и интернет-магазинов. Каждый наш кейс — это история комплексного подхода: от проектирования архитектуры и интеграции с 1С/ERP до поисковой оптимизации и вывода продукта на рынок. Изучите наши примеры работ, чтобы оценить качество кода, удобство интерфейсов и эффективность внедренных инструментов автоматизации.");
            w.text("cases-page-seo", "subtitle", "Опыт разработки и реальные результаты для вашего бизнеса");
            String[] seoLinks = {"Кейсы интернет-магазинов", "Разработка мобильных приложений", "Портфолио веб-порталов", "Примеры интеграции CRM"};
            for (int i = 0; i < seoLinks.length; i++)
                w.groupText("cases-page-seo-links", "link-" + (i + 1), "label", seoLinks[i]);

            w.marker("casesPageSeedV1");
        };
    }

    private record Project(String title, String category, String description, String href) { }
    private record Case(String projectGroup, String... filters) { }

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

        void text(String section, String key, String value) { add(section, null, key, "TEXT", value, null); }
        void link(String section, String key, String value, String href) { add(section, null, key, "LINK", value, href); }
        void groupText(String section, String group, String key, String value) { add(section, group, key, "TEXT", value, null); }
        void groupLink(String section, String group, String key, String value, String href) { add(section, group, key, "LINK", value, href); }
        void image(String section, String group, String key, String alt) { add(section, group, key, "IMAGE", alt, null); }

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
            repository.save(item);
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
