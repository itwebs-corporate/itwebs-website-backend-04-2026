package ru.itwebs.cms.bootstrap;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import ru.itwebs.cms.entity.ContentItem;
import ru.itwebs.cms.repository.ContentRepository;

/** Adds missing copy to both new and previously seeded databases once. */
@Configuration
public class ServicesCasesCopyContent {
    private static final String MARKER = "servicesCasesCopySeedV1";

    @Bean
    @Order(8)
    CommandLineRunner seedServicesCasesCopy(ContentRepository repository,
                                            PlatformTransactionManager transactions) {
        return args -> new TransactionTemplate(transactions).executeWithoutResult(ignored -> {
            if (repository.existsBySectionNameAndItemKeyAndGroupName("_system", MARKER, null)) return;
            int nextOrder = repository.findAll().stream()
                    .mapToInt(ContentItem::getSortOrder).max().orElse(-1) + 1;
            String[] words = {"разработки", "поддержки", "автоматизации"};
            for (int i = 0; i < words.length; i++) {
                add(repository, "services-page-title-words", "word-" + (i + 1), "word",
                        "Сменяемое слово заголовка", words[i], nextOrder++);
            }
            add(repository, "services-page", "automation", "title", "Заголовок автоматизации",
                    "Услуги ITWEBS — полный цикл автоматизации", nextOrder++);
            add(repository, "services-page-seo", null, "eyebrow", "Надзаголовок SEO-блока",
                    "Для поискового интента", nextOrder++);
            add(repository, "cases-page-seo", null, "eyebrow", "Надзаголовок SEO-блока",
                    "Для поискового интента", nextOrder++);
            add(repository, "_system", null, MARKER, "Services/cases copy seed", "complete", nextOrder);
        });
    }

    private void add(ContentRepository repository, String section, String group, String key,
                     String label, String value, int sortOrder) {
        if (repository.existsBySectionNameAndItemKeyAndGroupName(section, key, group)) return;
        ContentItem item = new ContentItem();
        item.setSectionName(section);
        item.setGroupName(group);
        item.setItemKey(key);
        item.setType("TEXT");
        item.setLabel(label);
        item.setValue(value);
        item.setSortOrder(sortOrder);
        item.setPublished(!section.equals("_system"));
        repository.save(item);
    }
}
