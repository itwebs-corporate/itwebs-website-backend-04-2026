package ru.itwebs.cms.service;

import org.springframework.stereotype.Service;
import ru.itwebs.cms.dto.PageResponse;
import ru.itwebs.cms.entity.ContentItem;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class PageService {
    private static final Set<String> SHARED = Set.of(
            "header", "contact", "footer", "footer-navigation", "footer-services", "footer-regions");
    private static final Set<String> SERVICES = Set.of(
            "services-page", "services-page-catalog", "services-page-stacks",
            "services-page-stack-items", "services-page-process", "services-page-process-steps",
            "services-page-tasks", "services-page-audience", "services-page-audiences",
            "services-page-cooperation", "services-page-cooperation-models",
            "services-page-projects", "services-page-project-items", "services-page-seo",
            "services-page-seo-topics", "service-categories", "project-cards");
    private static final Set<String> CASES = Set.of(
            "cases-page", "cases-page-filters", "cases-page-items", "cases-page-seo",
            "cases-page-seo-links", "project-cards");

    private final ContentService content;

    public PageService(ContentService content) {
        this.content = content;
    }

    public PageResponse services() {
        return page("services", SERVICES);
    }

    public PageResponse cases() {
        return page("cases", CASES, true);
    }

    private PageResponse page(String slug, Set<String> pageSections) {
        return page(slug, pageSections, false);
    }

    private PageResponse page(String slug, Set<String> pageSections, boolean resolveCases) {
        List<ContentItem> published = content.published(null);
        Set<String> included = new java.util.HashSet<>(pageSections);
        included.addAll(SHARED);

        Map<String, List<ContentItem>> bySection = published.stream()
                .filter(item -> included.contains(item.getSectionName()))
                .collect(Collectors.groupingBy(ContentItem::getSectionName));

        Map<String, PageResponse.Group> projects = groupItems(bySection.getOrDefault("project-cards", List.of()));
        Map<String, List<PageResponse.Group>> sections = new LinkedHashMap<>();
        bySection.keySet().stream().sorted().forEach(section -> {
            if (section.equals("project-cards")) return;
            List<PageResponse.Group> groups = groups(bySection.get(section));
            if (resolveCases && section.equals("cases-page-items")) {
                groups = groups.stream().map(group -> {
                    String projectGroup = value(group, "projectGroup");
                    return new PageResponse.Group(group.groupName(), group.fields(), projects.get(projectGroup));
                }).toList();
            } else if (section.equals("services-page-project-items")) {
                groups = groups.stream().map(group -> {
                    String projectGroup = value(group, "projectGroup");
                    return new PageResponse.Group(group.groupName(), group.fields(), projects.get(projectGroup));
                }).toList();
            }
            sections.put(section, groups);
        });

        if (resolveCases) sections.remove("project-cards");
        return new PageResponse(slug, sections);
    }

    private Map<String, PageResponse.Group> groupItems(List<ContentItem> items) {
        Map<String, PageResponse.Group> result = new LinkedHashMap<>();
        for (PageResponse.Group group : groups(items)) result.put(group.groupName(), group);
        return result;
    }

    private List<PageResponse.Group> groups(List<ContentItem> items) {
        Map<String, List<ContentItem>> byGroup = new LinkedHashMap<>();
        items.stream().sorted(Comparator.comparingInt(ContentItem::getSortOrder).thenComparing(ContentItem::getId))
                .forEach(item -> byGroup.computeIfAbsent(item.getGroupName(), ignored -> new ArrayList<>()).add(item));

        List<PageResponse.Group> result = new ArrayList<>();
        byGroup.forEach((groupName, fields) -> {
            Map<String, PageResponse.Field> mappedFields = new LinkedHashMap<>();
            fields.forEach(item -> {
                Long mediaId = item.getMedia() == null ? null : item.getMedia().getId();
                mappedFields.put(item.getItemKey(), new PageResponse.Field(
                        item.getId(), item.getType(), item.getLabel(), item.getValue(), item.getHref(),
                        mediaId, mediaId == null ? null : "/api/media/" + mediaId, item.getSortOrder()));
            });
            result.add(new PageResponse.Group(groupName, mappedFields, null));
        });
        return result;
    }

    private String value(PageResponse.Group group, String key) {
        PageResponse.Field field = group.fields().get(key);
        return field == null ? null : field.value();
    }
}
