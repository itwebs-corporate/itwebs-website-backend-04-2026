package ru.itwebs.cms.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.itwebs.cms.catalog.CatalogDtos.*;
import ru.itwebs.cms.catalog.CatalogService;
import ru.itwebs.cms.dto.PageResponse;
import ru.itwebs.cms.entity.*;
import ru.itwebs.cms.pages.PageDtos.*;
import ru.itwebs.cms.repository.*;
import java.util.*;

@Service
@Transactional(readOnly = true)
public class PageService {
    private final SitePageRepository pages;
    private final SiteBlockRepository blocks;
    private final PageCollectionRepository collections;
    private final CatalogRepository entries;
    private final CatalogService catalog;
    private final MediaRepository media;
    private final StructuredPayloadLimits limits;

    public PageService(SitePageRepository pages, SiteBlockRepository blocks, PageCollectionRepository collections,
                       CatalogRepository entries, CatalogService catalog, MediaRepository media, StructuredPayloadLimits limits) {
        this.pages = pages;
        this.blocks = blocks;
        this.collections = collections;
        this.entries = entries;
        this.catalog = catalog;
        this.media = media;
        this.limits = limits;
    }
    public List<PageCard> list() { return pages.findAllByOrderByIdAsc().stream().map(this::card).toList(); }
    private SitePage find(String slug) { return pages.findBySlug(slug).orElseThrow(() -> new NoSuchElementException("Страница не найдена")); }
    public AdminPage admin(String slug) {
        SitePage page = find(slug);
        return new AdminPage(card(page), page.seoTitle, page.seoDescription, page.canonical,
                pageBlocks(page, false).stream().map(this::block).toList(), page.collections.stream().map(this::collection).toList());
    }
    public PageResponse published(String slug) {
        SitePage page = find(slug);
        if (!page.published || slug.equals("shared")) throw new NoSuchElementException("Страница не найдена");
        List<SiteBlock> included = pageBlocks(page, true);
        Map<String, List<Card>> selections = new LinkedHashMap<>();
        page.collections.forEach(binding -> selections.put(binding.key, binding.allEntries
                ? catalog.list(binding.kind, true, null, 0, 100).items()
                : binding.entries.stream().filter(entry -> entry.published).map(entry -> displayCard(binding, entry)).toList()));
        Map<Long, MediaView> images = new TreeMap<>();
        included.forEach(item -> item.images.forEach(image -> images.put(image.getId(), new MediaView(image.getId(), "/api/media/" + image.getId(), image.getAltText()))));
        return new PageResponse(page.slug, page.path, page.title, page.seoTitle, page.seoDescription, page.canonical,
                included.stream().map(item -> publicBlock(item)).toList(), selections, new ArrayList<>(images.values()));
    }
    private List<SiteBlock> pageBlocks(SitePage page, boolean published) {
        if (page.slug.equals("shared")) return page.blocks.stream().filter(item -> !published || item.published).toList();
        List<SiteBlock> shared = pages.findBySlug("shared").filter(item -> !published || item.published).map(item -> item.blocks).orElse(List.of());
        List<SiteBlock> content = new ArrayList<>(page.blocks);
        content.addAll(page.reusedBlocks);
        List<SiteBlock> result = new ArrayList<>();
        shared.stream().filter(item -> item.key.equals("header")).forEach(result::add);
        content.stream().filter(item -> item.type != BlockType.SEO).forEach(result::add);
        if (!page.slug.equals("policy")) shared.stream().filter(item -> item.key.equals("contact")).forEach(result::add);
        content.stream().filter(item -> item.type == BlockType.SEO).forEach(result::add);
        shared.stream().filter(item -> !item.key.equals("header") && !item.key.equals("contact")).forEach(result::add);
        return result.stream().filter(item -> !published || item.published).toList();
    }
    @Transactional
    public AdminPage save(String slug, PageRequest request) {
        SitePage page = find(slug);
        CatalogService.version(page.version, request.version());
        safeHref(request.canonical());
        page.title = request.title().trim();
        page.seoTitle = request.seoTitle();
        page.seoDescription = request.seoDescription();
        page.canonical = request.canonical();
        page.published = request.published();
        pages.saveAndFlush(page);
        return admin(slug);
    }
    @Transactional
    public BlockView saveBlock(String slug, Long id, BlockRequest request) {
        limits.validate(request);
        SiteBlock item = blocks.findById(id).filter(value -> value.page.getSlug().equals(slug)).orElseThrow();
        CatalogService.version(item.version, request.version());
        validateValues(item.schema.fields(), request.data().fields());
        if (item.schema.itemFields().isEmpty() && !request.data().items().isEmpty()) throw new IllegalArgumentException("Этот блок не содержит списка");
        request.data().items().forEach(row -> validateValues(item.schema.itemFields(), row));
        Set<Long> ids = new TreeSet<>();
        collectImages(request.data().fields(), ids);
        request.data().items().forEach(row -> collectImages(row, ids));
        item.images.clear();
        ids.forEach(imageId -> item.images.add(media.findByIdForUpdate(imageId)
                .orElseThrow(() -> new IllegalArgumentException("Изображение не найдено: " + imageId))));
        item.data = request.data();
        item.published = request.published();
        return block(blocks.saveAndFlush(item));
    }
    private void collectImages(Map<String, Value> fields, Set<Long> ids) {
        fields.values().forEach(value -> { if (value.mediaId() != null) ids.add(value.mediaId()); });
    }
    private void validateValues(Map<String, FieldRule> schema, Map<String, Value> values) {
        if (!schema.keySet().equals(values.keySet())) throw new IllegalArgumentException("Набор полей блока фиксирован");
        values.forEach((key, value) -> {
            if (value == null) throw new IllegalArgumentException("Пустое поле блока");
            FieldRule rule = schema.get(key);
            if (!rule.type().equals("IMAGE") && value.mediaId() != null) throw new IllegalArgumentException("Изображение можно выбрать только в поле изображения");
            if (!rule.type().equals("LINK") && value.href() != null && !value.href().isBlank()) throw new IllegalArgumentException("Ссылка допустима только в поле ссылки");
            safeHref(value.href());
        });
    }
    public static void safeHref(String href) {
        if (href == null || href.isBlank()) return;
        if (href.chars().anyMatch(character -> Character.isISOControl(character) || character == '\\')) throw new IllegalArgumentException("Недопустимый адрес ссылки");
        if (!(href.startsWith("/") && !href.startsWith("//")) && !href.startsWith("#")
                && !href.startsWith("https://") && !href.startsWith("http://")
                && !href.startsWith("mailto:") && !href.startsWith("tel:")) throw new IllegalArgumentException("Недопустимый адрес ссылки");
    }
    @Transactional
    public CollectionView saveCollection(String slug, Long id, CollectionRequest request) {
        limits.validate(request);
        PageCollection binding = collections.findById(id).filter(value -> value.page.getSlug().equals(slug)).orElseThrow();
        CatalogService.version(binding.version, request.version());
        if (new HashSet<>(request.entryIds()).size() != request.entryIds().size()) throw new IllegalArgumentException("Записи не должны повторяться");
        if (request.allEntries() && !request.entryIds().isEmpty()) throw new IllegalArgumentException("Выберите все записи либо конкретный список");
        binding.entries.clear();
        request.entryIds().forEach(entryId -> binding.entries.add(entries.findById(entryId).filter(entry -> entry.kind == binding.kind)
                .orElseThrow(() -> new IllegalArgumentException("Запись не относится к выбранному каталогу"))));
        binding.allEntries = request.allEntries();
        List<DisplayOverride> overrides = request.overrides() == null ? List.of() : request.overrides();
        Set<Long> overridden = new HashSet<>();
        for (DisplayOverride override : overrides) {
            if (!request.entryIds().contains(override.entryId()) || !overridden.add(override.entryId()))
                throw new IllegalArgumentException("Переопределение должно относиться к одной выбранной записи");
            if (override.position() != null && !override.position().isBlank() && binding.kind != Kind.TEAM)
                throw new IllegalArgumentException("Должность можно переопределить только для участника команды");
            if (override.specialist() != null && binding.kind != Kind.SPECIALISTS)
                throw new IllegalArgumentException("Параметры специалиста можно переопределить только для специалиста");
        }
        binding.overrides = new ArrayList<>(overrides);
        return collection(collections.saveAndFlush(binding));
    }
    private PageCard card(SitePage page) { return new PageCard(page.id, page.version, page.slug, page.path, page.title, page.published); }
    private BlockView block(SiteBlock item) {
        return new BlockView(item.id, item.version, item.key, item.title, item.type, item.page.getSlug(), item.page.getSlug().equals("shared"), item.published, item.schema, item.data);
    }
    private BlockView publicBlock(SiteBlock item) {
        BlockData visible = new BlockData(visibleValues(item.data.fields()), item.data.items().stream()
                .filter(row -> row.values().stream().anyMatch(Value::visible)).map(this::visibleValues).toList());
        return new BlockView(item.id, item.version, item.key, item.title, item.type, item.page.getSlug(), item.page.getSlug().equals("shared"), true, item.schema, visible);
    }
    private Map<String, Value> visibleValues(Map<String, Value> values) {
        Map<String, Value> visible = new LinkedHashMap<>();
        values.forEach((key, value) -> visible.put(key, value.visible() ? value : new Value(null, null, null, false)));
        return visible;
    }
    private CollectionView collection(PageCollection item) {
        return new CollectionView(item.id, item.version, item.key, item.kind, item.allEntries, item.entries.stream().map(entry -> entry.id).toList(), item.overrides);
    }
    private Card displayCard(PageCollection binding, CatalogEntry entry) {
        Card card = catalog.card(entry);
        DisplayOverride override = binding.overrides.stream().filter(item -> item.entryId().equals(entry.id)).findFirst().orElse(null);
        if (override == null) return card;
        MediaView cover = card.cover();
        if (cover != null && override.imageAlt() != null) cover = new MediaView(cover.id(), cover.url(), override.imageAlt());
        MemberContent member = card.member();
        if (member != null && override.position() != null) member = new MemberContent(override.position(), member.biography());
        return new Card(card.id(), card.version(), card.kind(), override.title() == null ? card.title() : override.title(), card.slug(), card.link(),
                override.description() == null ? card.description() : override.description(), card.price(), card.published(), card.sortOrder(),
                cover, card.mobileCover(), card.categories(), override.specialist() == null ? card.specialist() : override.specialist(), member);
    }
}
