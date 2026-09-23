package ru.itwebs.cms.dto;

import ru.itwebs.cms.entity.ContentItem;

public record ContentResponse(Long id, String sectionName, String itemKey, String groupName,
                              String type, String label, String value, String href,
                              Long mediaId, String mediaUrl, int sortOrder, boolean published) {
    public static ContentResponse from(ContentItem item) {
        Long mediaId = item.getMedia() == null ? null : item.getMedia().getId();
        return new ContentResponse(item.getId(), item.getSectionName(), item.getItemKey(), item.getGroupName(),
                item.getType(), item.getLabel(), item.getValue(), item.getHref(), mediaId,
                mediaId == null ? null : "/api/media/" + mediaId, item.getSortOrder(), item.isPublished());
    }
}
