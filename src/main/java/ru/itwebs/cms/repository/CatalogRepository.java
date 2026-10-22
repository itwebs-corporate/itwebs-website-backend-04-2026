package ru.itwebs.cms.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.itwebs.cms.entity.CatalogEntry;
import ru.itwebs.cms.catalog.CatalogDtos.Kind;
import java.util.Optional;

public interface CatalogRepository extends JpaRepository<CatalogEntry, Long>, JpaSpecificationExecutor<CatalogEntry> {
    Optional<CatalogEntry> findByKindAndSlug(Kind kind, String slug);
    boolean existsByCategoriesId(Long id);
    boolean existsByImagesId(Long id);
    @Query("select count(c) from CaseStudy c where c.service.id = :id")
    long countServiceReferences(@Param("id") Long id);
    @Query("select count(s) from ServiceOffering s join s.examples c where c.id = :id")
    long countExampleReferences(@Param("id") Long id);
}
