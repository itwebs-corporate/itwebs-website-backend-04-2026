package ru.itwebs.cms.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.itwebs.cms.entity.SitePage;
import java.util.Optional;

public interface SitePageRepository extends JpaRepository<SitePage, Long> {
    Optional<SitePage> findBySlug(String slug);
    java.util.List<SitePage> findAllByOrderByIdAsc();
}
