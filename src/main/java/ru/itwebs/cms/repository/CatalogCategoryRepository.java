package ru.itwebs.cms.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.itwebs.cms.entity.CatalogCategory;
import ru.itwebs.cms.catalog.CatalogDtos.Kind;
import java.util.List;
import java.util.Optional;

public interface CatalogCategoryRepository extends JpaRepository<CatalogCategory, Long> {
    List<CatalogCategory> findByKindOrderBySortOrderAscIdAsc(Kind kind);
    Optional<CatalogCategory> findByKindAndSlug(Kind kind, String slug);
}
