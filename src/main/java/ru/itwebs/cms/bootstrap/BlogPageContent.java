package ru.itwebs.cms.bootstrap;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import ru.itwebs.cms.entity.BlogCategory;
import ru.itwebs.cms.entity.ContentItem;
import ru.itwebs.cms.repository.BlogCategoryRepository;
import ru.itwebs.cms.repository.ContentRepository;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Configuration
public class BlogPageContent {
    @Bean
    @Order(7)
    CommandLineRunner seedBlogPage(ContentRepository content, BlogCategoryRepository categories,
                                  PlatformTransactionManager transactions) {
        return args -> new TransactionTemplate(transactions).executeWithoutResult(ignored -> {
            if (content.existsBySectionNameAndItemKeyAndGroupName("_system", "blogPageSeedV1", null)) return;
            Writer w = new Writer(content);
            w.text("blog-page", null, "eyebrow", "# Код, который работает");
            w.text("blog-page", null, "title", "Делимся");
            w.text("blog-page", null, "description", "Статьи про цифровизацию бизнеса и сложную разработку, кейсы, анонсы ивентов с нашим участием и новости компании.");
            w.text("blog-page", null, "allLabel", "Все");
            w.text("blog-page", null, "defaultTag", "Разработка");
            w.text("blog-page", null, "readingTimeLabel", "минут чтение");
            w.text("blog-page", null, "emptyLabel", "Статьи пока не опубликованы");
            w.text("blog-page", null, "loadMoreLabel", "Показать ещё");
            w.link("blog-page", "floatingCta", "Обсудить проект", "/#lead-form");
            String[] words = {"опытом", "кейсами"};
            for (int i = 0; i < words.length; i++) w.text("blog-page-title-words", "word-" + (i + 1), "word", words[i]);
            w.text("blog-page-meta", null, "title", "Блог — делимся опытом — ITWEBS");
            w.text("blog-page-meta", null, "description", "Статьи ITWEBS про цифровизацию бизнеса, Web и Mobile разработку, CRM, E-commerce и AI.");
            w.text("blog-page-meta", null, "canonical", "/blog");
            w.text("blog-article-page", null, "relatedTitle", "Вам может быть интересно");
            w.text("blog-article-page", null, "servicesTitle", "Услуги которые мы предоставляем");
            w.text("blog-article-page", null, "defaultTag", "Разработка");
            w.text("blog-article-page", null, "readingTimeLabel", "минут чтение");
            w.link("blog-article-page", "floatingCta", "Обсудить проект", "/#lead-form");
            // Copy existing SEO defaults once; subsequent edits are independent.
            w.text("blog-page-seo", null, "eyebrow", "Для поискового интента");
            w.text("blog-page-seo", null, "title", "Аутстаффинг IT-специалистов в Москве под ключ: от подбора до интеграции в проект");
            w.copySection("outstaff-page-seo", "blog-page-seo");
            w.copySection("outstaff-page-seo-items", "blog-page-seo-items");
            w.text("blog-page-seo", null, "expandLabel", "Читать все");
            w.text("blog-page-seo", null, "collapseLabel", "Свернуть");
            w.text("blog-article-page-seo", null, "eyebrow", "Для поискового интента");
            w.text("blog-article-page-seo", null, "title", "Разработка сайтов в Москве под ключ: от идеи до первых клиентов");
            w.copySection("services-page-seo", "blog-article-page-seo");
            w.copySection("services-page-seo-topics", "blog-article-page-seo-topics");
            w.text("blog-article-page-seo", null, "expandLabel", "Читать все");
            w.text("blog-article-page-seo", null, "collapseLabel", "Свернуть");
            w.text("blog-article-service-items", "service-1", "serviceGroup", "web-service-1");
            w.text("blog-article-service-items", "service-2", "serviceGroup", "crm-service-1");
            w.text("blog-article-service-items", "service-3", "serviceGroup", "outstaff-service-1");
            Set<String> slugs = new HashSet<>();
            categories.findAll().forEach(category -> slugs.add(category.getSlug()));
            String[] names = {"Mobile", "Web", "CRM", "E-com", "AI"};
            String[] keys = {"mobile", "web", "crm", "e-com", "ai"};
            for (int i = 0; i < keys.length; i++) {
                if (slugs.contains(keys[i])) continue;
                BlogCategory category = new BlogCategory();
                category.setName(names[i]); category.setSlug(keys[i]); category.setSortOrder(i);
                categories.save(category);
            }
            w.text("_system", null, "blogPageSeedV1", "complete");
        });
    }

    private static class Writer {
        private final ContentRepository repository;
        private final List<ContentItem> initial;
        private final Set<String> existing = new HashSet<>();
        private int nextOrder;
        Writer(ContentRepository repository) {
            this.repository = repository;
            initial = repository.findAllByOrderBySectionNameAscSortOrderAscIdAsc();
            for (ContentItem item : initial) {
                existing.add(identity(item.getSectionName(), item.getGroupName(), item.getItemKey()));
                nextOrder = Math.max(nextOrder, item.getSortOrder() + 1);
            }
        }
        private String identity(String section, String group, String key) {
            return section + "\u0000" + (group == null ? "" : group) + "\u0000" + key;
        }
        void text(String section, String group, String key, String value) { add(section, group, key, "TEXT", value, null); }
        void link(String section, String key, String value, String href) { add(section, null, key, "LINK", value, href); }
        void copySection(String source, String target) {
            for (ContentItem item : initial) if (item.getSectionName().equals(source)) {
                ContentItem copy = add(target, item.getGroupName(), item.getItemKey(), item.getType(), item.getValue(), item.getHref());
                if (copy != null) {
                    copy.setLabel(item.getLabel());
                    copy.setMedia(item.getMedia());
                    copy.setPublished(item.isPublished());
                }
            }
        }
        ContentItem add(String section, String group, String key, String type, String value, String href) {
            if (!existing.add(identity(section, group, key))) return null;
            ContentItem item = new ContentItem();
            item.setSectionName(section); item.setGroupName(group); item.setItemKey(key);
            item.setType(type); item.setLabel(key); item.setValue(value); item.setHref(href);
            item.setSortOrder(nextOrder++); item.setPublished(!section.equals("_system"));
            return repository.save(item);
        }
    }
}
