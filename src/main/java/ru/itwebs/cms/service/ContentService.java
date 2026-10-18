package ru.itwebs.cms.service;

import org.springframework.stereotype.Service;
import ru.itwebs.cms.dto.ContentRequest;
import ru.itwebs.cms.entity.ContentItem;
import ru.itwebs.cms.repository.ContentRepository;
import java.util.List;

@Service
public class ContentService {
    private final ContentRepository content;
    public ContentService(ContentRepository content) { this.content = content; }
    public List<ContentItem> list() { return content.findAllByOrderBySectionNameAscSortOrderAscIdAsc(); }
    public List<ContentItem> published(String section) {
        return content.findByPublishedTrueOrderBySectionNameAscSortOrderAscIdAsc().stream()
                .filter(item -> section == null || section.isBlank() || item.getSectionName().equals(section)).toList();
    }
    public ContentItem find(Long id) { return content.findById(id).orElseThrow(); }
    public ContentItem findPublished(Long id) {
        ContentItem item = find(id);
        if (!item.isPublished()) throw new java.util.NoSuchElementException("Элемент не найден");
        return item;
    }
    public long count() { return content.count(); }
    public ContentItem create(ContentRequest request) { throw retired(); }
    public ContentItem update(Long id, ContentRequest request) { throw retired(); }
    public void delete(Long id) { throw retired(); }
    private org.springframework.web.server.ResponseStatusException retired() {
        return new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.GONE,
                "Архив старого контента доступен только для чтения. Используйте /api/admin/pages и /api/admin/catalog");
    }
}
