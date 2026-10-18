package ru.itwebs.cms.bootstrap;

import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.itwebs.cms.catalog.ServiceComponentService;
import ru.itwebs.cms.entity.ContentItem;
import ru.itwebs.cms.repository.ContentRepository;
import ru.itwebs.cms.repository.ServiceOfferingRepository;

@Component
@Order(110)
public class ServiceComponentsMigration implements CommandLineRunner {
    private static final String MARKER = "serviceComponentsV1";
    private final ContentRepository markers;
    private final ServiceOfferingRepository services;
    private final ServiceComponentService components;
    private final JdbcTemplate jdbc;

    public ServiceComponentsMigration(ContentRepository markers, ServiceOfferingRepository services,
                                      ServiceComponentService components, JdbcTemplate jdbc) {
        this.markers = markers;
        this.services = services;
        this.components = components;
        this.jdbc = jdbc;
    }

    @Override
    @Transactional
    public void run(String... args) {
        jdbc.execute("select pg_advisory_xact_lock(738245981)");
        if (markers.existsBySectionNameAndItemKeyAndGroupName("_system", MARKER, null)) return;
        services.findAllByOrderByIdAsc().forEach(components::migrate);
        services.flush();
        ContentItem marker = new ContentItem();
        marker.setSectionName("_system");
        marker.setItemKey(MARKER);
        marker.setLabel("Service component dictionaries migration");
        marker.setValue("complete");
        marker.setPublished(false);
        markers.save(marker);
    }
}
