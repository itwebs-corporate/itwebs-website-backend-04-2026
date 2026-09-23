package ru.itwebs.cms.repository;

import ru.itwebs.cms.entity.ContentItem;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ContentRepository extends JpaRepository<ContentItem, Long> {
    List<ContentItem> findAllByOrderBySectionNameAscSortOrderAscIdAsc();
    List<ContentItem> findByPublishedTrueOrderBySectionNameAscSortOrderAscIdAsc();
    boolean existsBySectionNameAndItemKeyAndGroupName(String sectionName, String itemKey, String groupName);
    long countByMediaId(Long mediaId);
}
