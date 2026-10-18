package ru.itwebs.cms.bootstrap;

import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.itwebs.cms.entity.ContentItem;
import ru.itwebs.cms.pages.PageDtos.Value;
import ru.itwebs.cms.repository.ContentRepository;
import ru.itwebs.cms.repository.SiteBlockRepository;
import ru.itwebs.cms.repository.SitePageRepository;

@Component
@Order(120)
public class PolicyStructureMigration implements CommandLineRunner {
    private static final String MARKER = "policyStructureV1";
    private final ContentRepository markers;
    private final SitePageRepository pages;
    private final SiteBlockRepository blocks;
    private final JdbcTemplate jdbc;

    public PolicyStructureMigration(ContentRepository markers, SitePageRepository pages, SiteBlockRepository blocks, JdbcTemplate jdbc) {
        this.markers = markers;
        this.pages = pages;
        this.blocks = blocks;
        this.jdbc = jdbc;
    }

    @Override
    @Transactional
    public void run(String... args) {
        jdbc.execute("select pg_advisory_xact_lock(738245981)");
        if (markers.existsBySectionNameAndItemKeyAndGroupName("_system", MARKER, null)) return;
        pages.findBySlug("policy").ifPresent(page -> page.blocks.forEach(block -> {
            if (!block.key.startsWith("policy-section-")) return;
            Value title = block.data.fields().get("title");
            Value body = block.data.fields().get("body");
            if (title != null && title.text() != null && title.text().startsWith("1. Общие положения2. Основные понятия")
                    && (body == null || body.text() == null || body.text().isBlank())) {
                block.title = "Оглавление политики (архив переноса)";
                block.published = false;
            }
        }));
        blocks.flush();
        ContentItem marker = new ContentItem();
        marker.setSectionName("_system");
        marker.setItemKey(MARKER);
        marker.setLabel("Policy structure migration");
        marker.setValue("complete");
        marker.setPublished(false);
        markers.save(marker);
    }
}
