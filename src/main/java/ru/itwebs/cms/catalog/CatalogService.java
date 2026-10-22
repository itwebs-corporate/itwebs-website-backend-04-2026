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
    private final ServiceComponentService components;

    public CatalogService(CatalogRepository entries, CatalogCategoryRepository categories, MediaRepository media,
                          ru.itwebs.cms.service.StructuredPayloadLimits limits, ServiceComponentService components) {
        this.entries = entries;
        this.categories = categories;
        this.media = media;
        this.limits = limits;
        this.components = components;
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
            ServiceContent content = request.service();
            if (!content.faq().isEmpty() || !content.stack().isEmpty() || !content.process().isEmpty() || !content.tasks().isEmpty()
                    || !content.audience().isEmpty() || !content.cooperation().isEmpty())
                throw new IllegalArgumentException("Выберите элементы справочников в serviceSelections; списки внутри service должны быть пустыми");
            service.body = request.service();
            components.replace(service, request.serviceSelections());
            if (new HashSet<>(request.caseIds()).size() != request.caseIds().size()) throw new IllegalArgumentException("Примеры работ не должны повторяться");
            service.examples.clear();
            request.caseIds().forEach(caseId -> service.examples.add((CaseStudy) find(Kind.CASES, caseId)));
        } else {
            if (request.serviceSelections() != null) throw new IllegalArgumentException("Справочники услуги допустимы только у услуги");
            if (!request.caseIds().isEmpty()) throw new IllegalArgumentException("Примеры работ задаются только у услуги");
            if (entry instanceof CaseStudy project) {
                if (request.project() == null) throw new IllegalArgumentException("Заполните содержимое кейса");
                saveProject(project, request.project());
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
        if (request.canonical() != null) {
            ru.itwebs.cms.service.PageService.safeCanonical(request.canonical());
            entry.canonical = blank(request.canonical()) ? null : request.canonical();
        }
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
        imageIds.forEach(imageId -> entry.images.add(ru.itwebs.cms.service.MediaFormats.requireImage(media.findByIdForUpdate(imageId)
                .orElseThrow(() -> new IllegalArgumentException("Изображение не найдено: " + imageId)))));
        entry.cover = request.coverMediaId() == null ? null : entry.images.stream()
                .filter(image -> image.getId().equals(request.coverMediaId())).findFirst().orElseThrow();
        entry.mobileCover = request.mobileCoverMediaId() == null ? null : entry.images.stream()
                .filter(image -> image.getId().equals(request.mobileCoverMediaId())).findFirst().orElseThrow();
        return view(entries.saveAndFlush(entry), false);
    }

    private void saveProject(CaseStudy project, CaseContent request) {
        CaseContent previous = project.body;
        // Existing clients send only the original fields. Preserve additions on their round trip.
        boolean legacy = request.sectionsV2() == null && request.tags() == null && request.serviceId() == null && request.seo() == null;
        List<CaseSection> sections = request.sectionsV2() != null ? request.sectionsV2()
                : previous == null || previous.sectionsV2() == null ? List.of() : previous.sectionsV2();
        List<String> tags = request.tags() != null ? request.tags().stream().map(String::trim).distinct().toList()
                : previous == null || previous.tags() == null ? List.of() : previous.tags();
        CaseSeo seo = legacy && previous != null ? previous.seo() : request.seo();
        if (!legacy) project.service = request.serviceId() == null ? null : (ServiceOffering) find(Kind.SERVICES, request.serviceId());
        for (CaseSection section : sections) {
            boolean numbers = section.kind() == CaseSectionKind.STATS || section.kind() == CaseSectionKind.RESULTS;
            if (section.kind() == CaseSectionKind.SERVICE && project.service == null)
                throw new IllegalArgumentException("Для секции service выберите услугу в project.serviceId");
            if (Set.of(CaseSectionKind.STACK, CaseSectionKind.GALLERY, CaseSectionKind.SERVICE).contains(section.kind()) && !section.items().isEmpty())
                throw new IllegalArgumentException("Секции stack, gallery и service используют project.stack, project.gallery и project.serviceId; items должен быть пустым");
            if (numbers && section.items().isEmpty()) throw new IllegalArgumentException("Заполните показатели секции stats/results");
            for (CaseSectionItem item : section.items()) {
                if (numbers && (blank(item.value()) || blank(item.description())))
                    throw new IllegalArgumentException("У показателя обязательны value и description; единица задаётся отдельно в unit");
                if (!numbers && (!blank(item.value()) || !blank(item.unit())))
                    throw new IllegalArgumentException("value и unit допустимы только в stats/results");
                if (!numbers && blank(item.title()) && blank(item.description()))
                    throw new IllegalArgumentException("Заполните заголовок или описание карточки секции");
            }
        }
        if (seo != null) seo.links().forEach(link -> ru.itwebs.cms.service.PageService.safeHref(link.href()));
        // The service association is stored as an FK, never duplicated in JSON.
        project.body = new CaseContent(request.headline(), request.client(), request.challenge(), request.solution(), request.result(),
                request.sections(), request.stack(), request.gallery(), sections, tags, null, null, seo);
        limits.validate(project.body);
    }
    private CaseContent projectContent(CaseStudy project, boolean published) {
        CaseContent body = project.body;
        if (body == null) return null;
        boolean visibleService = project.service != null && (!published || project.service.published);
        List<CaseSection> sections = body.sectionsV2() == null ? List.of() : body.sectionsV2();
        if (published && !visibleService) sections = sections.stream().filter(section -> section.kind() != CaseSectionKind.SERVICE).toList();
        return new CaseContent(body.headline(), body.client(), body.challenge(), body.solution(), body.result(), body.sections(), body.stack(), body.gallery(),
                sections, body.tags() == null ? List.of() : body.tags(), visibleService ? project.service.id : null,
                visibleService ? project.service.slug : null, body.seo());
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

    public Card card(CatalogEntry entry) { return card(entry, true); }

    private Card card(CatalogEntry entry, boolean resolveCover) {
        String prefix = switch (entry.kind) {
            case SERVICES -> "/services/";
            case CASES -> "/cases/";
            case SPECIALISTS -> "/outstaff/";
            case TEAM -> "/team/";
        };
        return new Card(entry.id, entry.version, entry.kind, entry.title, entry.slug, blank(entry.canonical) ? prefix + entry.slug : entry.canonical,
                entry.description, entry.price, entry.published, entry.sortOrder, resolveCover ? cover(entry) : image(entry.cover), image(entry.mobileCover),
                entry.categories.stream().sorted(Comparator.comparingInt((CatalogCategory item) -> item.sortOrder).thenComparing(item -> item.id))
                        .map(this::category).toList(),
                entry instanceof OutstaffSpecialist specialist ? specialist.body : null,
                entry instanceof TeamMember member ? member.body : null);
    }

    private EntryView view(CatalogEntry entry, boolean published) {
        Map<Long, MediaAsset> images = new TreeMap<>();
        entry.images.forEach(image -> images.put(image.getId(), image));
        if (entry instanceof ServiceOffering service) components.images(service).forEach(image -> images.put(image.getId(), image));
        return new EntryView(card(entry, published), published && blank(entry.seoTitle) ? entry.title : entry.seoTitle,
                published && blank(entry.seoDescription) ? entry.description : entry.seoDescription,
                entry instanceof ServiceOffering service ? components.resolve(service) : null,
                entry instanceof CaseStudy project ? projectContent(project, published) : null,
                entry instanceof OutstaffSpecialist specialist ? specialist.body : null,
                entry instanceof TeamMember member ? member.body : null,
                entry instanceof ServiceOffering service ? service.examples.stream()
                        .filter(project -> !published || project.published).map(this::card).toList() : List.of(),
                images.values().stream().map(this::image).toList(),
                entry instanceof ServiceOffering service ? components.selections(service) : null,
                blank(entry.canonical) ? (!published || entry.kind == Kind.SERVICES ? null : card(entry).link()) : entry.canonical,
                entry instanceof CaseStudy project && project.service != null && (!published || project.service.published)
                        ? card(project.service) : null);
    }

    private boolean blank(String value) { return value == null || value.isBlank(); }
    private MediaView cover(CatalogEntry entry) {
        if (entry.cover != null) return image(entry.cover);
        if (entry instanceof CaseStudy project && project.body != null) {
            for (Picture picture : project.body.gallery()) {
                if (picture.mediaId() != null) {
                    MediaAsset asset = entry.images.stream().filter(value -> picture.mediaId().equals(value.getId())).findFirst().orElse(null);
                    if (asset != null) return new MediaView(asset.getId(), "/api/media/" + asset.getId(), blank(picture.alt()) ? asset.getAltText() : picture.alt());
                }
                if (!blank(picture.sourceUrl())) return new MediaView(null, picture.sourceUrl(), picture.alt());
            }
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
        if (kind == Kind.SERVICES && entries.countServiceReferences(id) > 0) throw new BlogConflictException("Услуга связана с кейсом. Сначала уберите связь или скройте услугу");
        if (entries.countExampleReferences(id) > 0) throw new BlogConflictException("Кейс используется в примерах работ услуги");
        entries.delete(entry);
        entries.flush();
    }
    public static void version(Long actual, Long requested) {
        if (requested == null || !Objects.equals(actual, requested)) throw new BlogConflictException("Запись изменена. Откройте её заново");
    }
}
