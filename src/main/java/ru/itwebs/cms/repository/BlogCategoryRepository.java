package ru.itwebs.cms.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.itwebs.cms.entity.BlogCategory;
import java.util.List;

public interface BlogCategoryRepository extends JpaRepository<BlogCategory, Long> {
    boolean existsBySlugAndIdNot(String slug, Long id);
    List<BlogCategory> findAllByOrderBySortOrderAscIdAsc();
}
