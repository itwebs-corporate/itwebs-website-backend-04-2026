package ru.itwebs.cms.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.itwebs.cms.entity.SiteBlock;

public interface SiteBlockRepository extends JpaRepository<SiteBlock, Long> {
    boolean existsByImagesId(Long id);
}
