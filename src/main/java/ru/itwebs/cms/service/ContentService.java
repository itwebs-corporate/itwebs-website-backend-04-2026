package ru.itwebs.cms.service;

import org.springframework.stereotype.Service;
import ru.itwebs.cms.dto.ContentRequest;
import ru.itwebs.cms.entity.ContentItem;
import ru.itwebs.cms.repository.ContentRepository;
import ru.itwebs.cms.repository.MediaRepository;
import java.util.List;

@Service
public class ContentService {
    private final ContentRepository content;
    private final MediaRepository media;
    public ContentService(ContentRepository content, MediaRepository media) {
        this.content = content; this.media = media;
    }
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
    public ContentItem create(ContentRequest request) { return save(new ContentItem(), request); }
    public ContentItem update(Long id, ContentRequest request) { return save(find(id), request); }
    private ContentItem save(ContentItem item, ContentRequest request) {
        if (request.href() != null && !request.href().isBlank() && !safeHref(request.href()))
            throw new IllegalArgumentException("Недопустимый адрес ссылки");
        item.setSectionName(request.sectionName().trim());
        item.setItemKey(request.itemKey().trim());
        item.setGroupName(request.groupName());
        item.setType(request.type());
        item.setLabel(request.label());
        item.setValue(request.value());
        item.setHref(request.href());
        item.setMedia(request.mediaId() == null ? null : media.findById(request.mediaId()).orElseThrow());
        item.setSortOrder(request.sortOrder());
        item.setPublished(request.published());
        return content.save(item);
    }
    private boolean safeHref(String href) {
        return (href.startsWith("/") && !href.startsWith("//")) || href.startsWith("#") ||
                href.startsWith("https://") || href.startsWith("http://") ||
                href.startsWith("mailto:") || href.startsWith("tel:");
    }
    public void delete(Long id) { content.deleteById(id); }
}
