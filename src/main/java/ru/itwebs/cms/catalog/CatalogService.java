package ru.itwebs.cms.catalog;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.itwebs.cms.blog.BlogConflictException;
import ru.itwebs.cms.entity.*;
import ru.itwebs.cms.repository.*;
import java.util.*;
import static ru.itwebs.cms.catalog.CatalogDtos.*;

@Service
@Transactional(readOnly = true)
public class CatalogService {
    private final CatalogRepository entries;
    private final CatalogCategoryRepository categories;
    private final MediaRepository media;
    private final ru.itwebs.cms.service.StructuredPayloadLimits limits;

    public CatalogService(CatalogRepository entries, CatalogCategoryRepository categories, MediaRepository media,
                          ru.itwebs.cms.service.StructuredPayloadLimits limits) {
        this.entries = entries;
        this.categories = categories;
        this.media = media;
        this.limits = limits;
    }

    public static Kind kind(String value) {
        try { return Kind.valueOf(value.toUpperCase(Locale.ROOT)); }
        catch (IllegalArgumentException error) { throw new NoSuchElementException("Каталог не найден"); }
    }

    public EntryPage list(Kind kind, boolean published, String category, int page, int size) {
        if (page < 0 || page > 100000 || size < 1 || size > 100) throw new IllegalArgumentException("page: 0–100000, size: 1–100");
        Specification<CatalogEntry> spec = (root, query, builder) -> builder.equal(root.get("kind"), kind);
        if (published) spec = spec.and((root, query, builder) -> builder.isTrue(root.get("published")));
        if (category != null && !category.isBlank()) {
            CatalogCategory selected = categories.findByKindAndSlug(kind, category).orElse(null);
            if (selected == null) return new EntryPage(List.of(), page, size, 0, 0);
            spec = spec.and((root, query, builder) -> builder.isMember(selected, root.get("categories")));
        }
        var result = entries.findAll(spec, PageRequest.of(page, size, Sort.by("sortOrder", "id")));
        return new EntryPage(result.getContent().stream().map(this::card).toList(), page, size, result.getTotalElements(), result.getTotalPages());
    }

    public EntryView published(Kind kind, String slug) {
        CatalogEntry entry = entries.findByKindAndSlug(kind, slug).filter(item -> item.published)
                .orElseThrow(() -> new NoSuchElementException("Запись не найдена"));
        return view(entry, true);
    }

    public EntryView get(Kind kind, Long id) { return view(find(kind, id), false); }

    private CatalogEntry find(Kind kind, Long id) {
        return entries.findById(id).filter(entry -> entry.kind == kind)
                .orElseThrow(() -> new NoSuchElementException("Запись не найдена"));
    }

    @Transactional
    public EntryView save(Kind kind, Long id, EntryRequest request) {
        limits.validate(request);
        CatalogEntry entry = id == null ? create(kind) : find(kind, id);
        if (id != null) version(entry.version, request.version());
        CatalogEntry duplicate = entries.findByKindAndSlug(kind, request.slug()).orElse(null);
        if (duplicate != null && !Objects.equals(duplicate.id, id)) throw new BlogConflictException("Адрес уже занят");
        int bodies = (request.service() == null ? 0 : 1) + (request.project() == null ? 0 : 1)
                + (request.specialist() == null ? 0 : 1) + (request.member() == null ? 0 : 1);
        if (bodies != 1) throw new IllegalArgumentException("Передайте содержимое только выбранного типа записи");
        if (entry instanceof ServiceOffering service) {
            if (request.service() == null) throw new IllegalArgumentException("Заполните содержимое услуги");
            service.body = request.service();
            if (new HashSet<>(request.caseIds()).size() != request.caseIds().size()) throw new IllegalArgumentException("Примеры работ не должны повторяться");
            service.examples.clear();
            request.caseIds().forEach(caseId -> service.examples.add((CaseStudy) find(Kind.CASES, caseId)));
        } else {
            if (!request.caseIds().isEmpty()) throw new IllegalArgumentException("Примеры работ задаются только у услуги");
            if (entry instanceof CaseStudy project) {
                if (request.project() == null) throw new IllegalArgumentException("Заполните содержимое кейса");
                project.body = request.project();
            } else if (entry instanceof OutstaffSpecialist specialist) {
                if (request.specialist() == null) throw new IllegalArgumentException("Заполните данные специалиста");
                specialist.body = request.specialist();
            } else if (entry instanceof TeamMember member) {
                if (request.member() == null) throw new IllegalArgumentException("Заполните данные сотрудника");
                member.body = request.member();
            }
        }
        entry.kind = kind;
        entry.title = request.title().trim();
        entry.slug = request.slug();
        entry.description = request.description();
        entry.price = request.price();
        entry.seoTitle = request.seoTitle();
        entry.seoDescription = request.seoDescription();
        entry.published = request.published();
        entry.sortOrder = request.sortOrder();
        entry.categories.clear();
        request.categoryIds().stream().sorted().forEach(categoryId -> {
            CatalogCategory category = categories.findById(categoryId).filter(value -> value.kind == kind)
                    .orElseThrow(() -> new IllegalArgumentException("Категория не относится к этому каталогу"));
            entry.categories.add(category);
        });
        Set<Long> imageIds = new TreeSet<>();
        if (request.coverMediaId() != null) imageIds.add(request.coverMediaId());
        if (request.mobileCoverMediaId() != null) imageIds.add(request.mobileCoverMediaId());
        if (request.service() != null) request.service().stack().forEach(item -> addImage(imageIds, item.mediaId()));
        if (request.project() != null) {
            request.project().stack().forEach(item -> addImage(imageIds, item.mediaId()));
            request.project().gallery().forEach(item -> {
                if (item.mediaId() == null && (item.sourceUrl() == null || item.sourceUrl().isBlank()))
                    throw new IllegalArgumentException("Выберите изображение галереи или укажите его адрес");
                if (item.sourceUrl() != null && !item.sourceUrl().isBlank()
                        && !item.sourceUrl().startsWith("https://") && !item.sourceUrl().startsWith("http://"))
                    throw new IllegalArgumentException("Адрес изображения должен начинаться с http:// или https://");
                ru.itwebs.cms.service.PageService.safeHref(item.sourceUrl());
                addImage(imageIds, item.mediaId());
            });
        }
        entry.images.clear();
        imageIds.forEach(imageId -> entry.images.add(media.findByIdForUpdate(imageId)
                .orElseThrow(() -> new IllegalArgumentException("Изображение не найдено: " + imageId))));
        entry.cover = request.coverMediaId() == null ? null : entry.images.stream()
                .filter(image -> image.getId().equals(request.coverMediaId())).findFirst().orElseThrow();
        entry.mobileCover = request.mobileCoverMediaId() == null ? null : entry.images.stream()
                .filter(image -> image.getId().equals(request.mobileCoverMediaId())).findFirst().orElseThrow();
        return view(entries.saveAndFlush(entry), false);
    }

    private void addImage(Set<Long> ids, Long id) { if (id != null) ids.add(id); }

    private CatalogEntry create(Kind kind) {
        return switch (kind) {
            case SERVICES -> new ServiceOffering();
            case CASES -> new CaseStudy();
            case SPECIALISTS -> new OutstaffSpecialist();
            case TEAM -> new TeamMember();
        };
    }

    public Card card(CatalogEntry entry) {
        String prefix = switch (entry.kind) {
            case SERVICES -> "/services/";
            case CASES -> "/cases/";
            case SPECIALISTS -> "/outstaff/";
            case TEAM -> "/team/";
        };
        return new Card(entry.id, entry.version, entry.kind, entry.title, entry.slug, prefix + entry.slug,
                entry.description, entry.price, entry.published, entry.sortOrder, cover(entry), image(entry.mobileCover),
                entry.categories.stream().sorted(Comparator.comparingInt((CatalogCategory item) -> item.sortOrder).thenComparing(item -> item.id))
                        .map(this::category).toList(),
                entry instanceof OutstaffSpecialist specialist ? specialist.body : null,
                entry instanceof TeamMember member ? member.body : null);
    }

    private EntryView view(CatalogEntry entry, boolean published) {
        return new EntryView(card(entry), blank(entry.seoTitle) ? entry.title : entry.seoTitle,
                blank(entry.seoDescription) ? entry.description : entry.seoDescription,
                entry instanceof ServiceOffering service ? service.body : null,
                entry instanceof CaseStudy project ? project.body : null,
                entry instanceof OutstaffSpecialist specialist ? specialist.body : null,
                entry instanceof TeamMember member ? member.body : null,
                entry instanceof ServiceOffering service ? service.examples.stream()
                        .filter(project -> !published || project.published).map(this::card).toList() : List.of(),
                entry.images.stream().sorted(Comparator.comparing(MediaAsset::getId)).map(this::image).toList());
    }

    private boolean blank(String value) { return value == null || value.isBlank(); }
    private MediaView cover(CatalogEntry entry) {
        if (entry.cover != null) return image(entry.cover);
        if (entry instanceof CaseStudy project && project.body != null) {
            return project.body.gallery().stream().filter(picture -> !blank(picture.sourceUrl()))
                    .map(picture -> new MediaView(null, picture.sourceUrl(), picture.alt())).findFirst().orElse(null);
        }
        return null;
    }
    private MediaView image(MediaAsset asset) {
        return asset == null ? null : new MediaView(asset.getId(), "/api/media/" + asset.getId(), asset.getAltText());
    }
    private CategoryView category(CatalogCategory category) {
        return new CategoryView(category.id, category.version, category.kind, category.name, category.slug, category.sortOrder);
    }
    public List<CategoryView> categories(Kind kind) {
        return categories.findByKindOrderBySortOrderAscIdAsc(kind).stream().map(this::category).toList();
    }
    public CategoryView category(Kind kind, Long id) {
        return category(categories.findById(id).filter(value -> value.kind == kind).orElseThrow());
    }
    @Transactional
    public CategoryView saveCategory(Kind kind, Long id, CategoryRequest request) {
        CatalogCategory category = id == null ? new CatalogCategory() : categories.findById(id).filter(value -> value.kind == kind).orElseThrow();
        if (id != null) version(category.version, request.version());
        category.kind = kind;
        category.name = request.name().trim();
        category.slug = request.slug();
        category.sortOrder = request.sortOrder();
        return category(categories.saveAndFlush(category));
    }
    @Transactional
    public void deleteCategory(Kind kind, Long id) {
        CatalogCategory category = categories.findById(id).filter(value -> value.kind == kind).orElseThrow();
        if (entries.existsByCategoriesId(id)) throw new BlogConflictException("Категория используется в записях");
        categories.delete(category);
        categories.flush();
    }
    @Transactional
    public void delete(Kind kind, Long id) {
        CatalogEntry entry = find(kind, id);
        if (entries.countExampleReferences(id) > 0) throw new BlogConflictException("Кейс используется в примерах работ услуги");
        entries.delete(entry);
        entries.flush();
    }
    public static void version(Long actual, Long requested) {
        if (requested == null || !Objects.equals(actual, requested)) throw new BlogConflictException("Запись изменена. Откройте её заново");
    }
}
