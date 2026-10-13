package ru.itwebs.cms.blog;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.itwebs.cms.entity.*;
import ru.itwebs.cms.repository.*;
import java.time.Instant;
import java.util.*;
import static ru.itwebs.cms.blog.BlogDtos.*;

@Service
@Transactional(readOnly = true)
public class BlogService {
    private final BlogArticleRepository articles;
    private final BlogCategoryRepository categories;
    private final MediaRepository media;
    private final ObjectMapper mapper;

    public BlogService(BlogArticleRepository articles, BlogCategoryRepository categories,
                       MediaRepository media, ObjectMapper mapper) {
        this.articles = articles; this.categories = categories; this.media = media; this.mapper = mapper;
    }

    public List<CategoryResponse> categories() {
        return categories.findAllByOrderBySortOrderAscIdAsc().stream().map(this::categoryResponse).toList();
    }

    public CategoryResponse category(Long id) { return categoryResponse(findCategory(id)); }

    @Transactional
    public CategoryResponse createCategory(CategoryRequest request) {
        if (request.version() != null) throw new IllegalArgumentException("При создании version не передаётся");
        BlogCategory category = new BlogCategory();
        applyCategory(category, request);
        return categoryResponse(categories.saveAndFlush(category));
    }

    @Transactional
    public CategoryResponse updateCategory(Long id, CategoryRequest request) {
        BlogCategory category = findCategory(id);
        checkVersion(category.getVersion(), request.version());
        applyCategory(category, request);
        return categoryResponse(categories.saveAndFlush(category));
    }

    @Transactional
    public void deleteCategory(Long id) {
        BlogCategory category = findCategory(id);
        if (articles.existsByCategoriesId(id)) throw new BlogConflictException("Категория используется в статьях; сначала уберите привязки");
        categories.delete(category);
        categories.flush();
    }

    private void applyCategory(BlogCategory category, CategoryRequest request) {
        if (request.slug().equals("all")) throw new IllegalArgumentException("Slug all зарезервирован для фильтра всех статей");
        if (categories.existsBySlugAndIdNot(request.slug(), category.getId() == null ? 0L : category.getId()))
            throw new BlogConflictException("Slug категории уже занят");
        category.setName(request.name().trim());
        category.setSlug(request.slug());
        category.setSortOrder(request.sortOrder());
    }

    public ArticlePage list(boolean publicOnly, int page, int size, String categorySlug, String status) {
        validatePage(page, size);
        if (status != null && !Set.of("DRAFT", "PUBLISHED").contains(status))
            throw new IllegalArgumentException("Статус должен быть DRAFT или PUBLISHED");
        String filter = categorySlug == null || categorySlug.isBlank() ? null : categorySlug;
        if (filter != null && (filter.length() > 80 || !filter.matches("[a-z0-9]+(?:-[a-z0-9]+)*")))
            throw new IllegalArgumentException("Некорректный slug категории");
        Sort sort = publicOnly
                ? Sort.by(Sort.Order.desc("publishedAt"), Sort.Order.desc("id"))
                : Sort.by(Sort.Order.desc("updatedAt"), Sort.Order.desc("id"));
        var result = articles.findCards(publicOnly, Instant.now(), publicOnly ? null : status, filter, null, PageRequest.of(page, size, sort));
        return new ArticlePage(cards(result.getContent()), page, size,
                result.getTotalElements(), result.getTotalPages());
    }

    public ArticleResponse article(Long id) { return response(findArticle(id)); }

    public ArticleResponse published(String slug) { return response(findPublished(slug)); }

    public List<Card> related(String slug, int limit) {
        if (limit < 1 || limit > 12) throw new IllegalArgumentException("limit должен быть от 1 до 12");
        Instant now = Instant.now();
        Long id = articles.findPublishedId(slug, now)
                .orElseThrow(() -> new NoSuchElementException("Опубликованная статья не найдена"));
        var result = articles.findCards(true, now, null, null, id, PageRequest.of(0, limit,
                Sort.by(Sort.Order.desc("publishedAt"), Sort.Order.desc("id"))));
        return cards(result.getContent());
    }

    @Transactional
    public ArticleResponse create(ArticleRequest request) {
        if (request.version() != null) throw new IllegalArgumentException("При создании version не передаётся");
        BlogArticle article = new BlogArticle();
        apply(article, request);
        return response(articles.saveAndFlush(article));
    }

    @Transactional
    public ArticleResponse update(Long id, ArticleRequest request) {
        BlogArticle article = findArticle(id);
        checkVersion(article.getVersion(), request.version());
        apply(article, request);
        return response(articles.saveAndFlush(article));
    }

    @Transactional
    public void delete(Long id) {
        articles.delete(findArticle(id));
        articles.flush();
    }

    private void apply(BlogArticle article, ArticleRequest request) {
        if (articles.existsBySlugAndIdNot(request.slug(), article.getId() == null ? 0L : article.getId()))
            throw new BlogConflictException("Slug статьи уже занят");
        Set<BlogCategory> selected = new LinkedHashSet<>();
        for (Long id : request.categoryIds()) selected.add(findCategory(id));
        Set<Long> imageIds = new TreeSet<>();
        if (request.coverMediaId() != null) imageIds.add(request.coverMediaId());
        long words = 0;
        for (Block block : request.blocks()) {
            validateBlock(block);
            if (block.mediaId() != null) imageIds.add(block.mediaId());
            if (block.text() != null) words += wordCount(block.text());
            if (block.items() != null) for (String item : block.items()) words += wordCount(item);
        }
        // Lock referenced media in a stable order so concurrent deletion cannot remove their files.
        Map<Long, MediaAsset> lockedImages = new LinkedHashMap<>();
        for (Long id : imageIds) lockedImages.put(id, findMedia(id));
        Set<MediaAsset> images = new LinkedHashSet<>(lockedImages.values());
        MediaAsset cover = request.coverMediaId() == null ? null : lockedImages.get(request.coverMediaId());
        String excerpt = request.excerpt() == null ? "" : request.excerpt().trim();
        if (request.status().equals("PUBLISHED") && (request.blocks().isEmpty() || cover == null || excerpt.isBlank() || selected.isEmpty()))
            throw new IllegalArgumentException("Для публикации нужны содержимое, обложка, описание и хотя бы одна категория");
        String body;
        try { body = mapper.writeValueAsString(request.blocks()); }
        catch (JsonProcessingException error) { throw new IllegalArgumentException("Некорректные блоки статьи"); }
        if (body.length() > 1_000_000) throw new IllegalArgumentException("Содержимое статьи превышает 1 000 000 символов");
        article.setTitle(request.title().trim());
        article.setSlug(request.slug());
        article.setExcerpt(excerpt);
        article.setBodyJson(body);
        article.setCover(cover);
        article.setCategories(selected);
        article.setImages(images);
        article.setStatus(request.status());
        Instant publication = request.publishedAt() != null ? request.publishedAt() : article.getPublishedAt();
        if (request.status().equals("PUBLISHED") && publication == null) publication = Instant.now();
        article.setPublishedAt(publication);
        article.setReadingMinutes(request.readingMinutes() == null ? (int) Math.max(1, (words + 199) / 200) : request.readingMinutes());
        article.setSeoTitle(trimNullable(request.seoTitle()));
        article.setSeoDescription(trimNullable(request.seoDescription()));
    }

    private void validateBlock(Block block) {
        boolean textType = Set.of("HEADING", "PARAGRAPH", "QUOTE").contains(block.type());
        boolean listType = Set.of("BULLET_LIST", "NUMBERED_LIST").contains(block.type());
        boolean imageType = block.type().equals("IMAGE");
        if (textType && (block.text() == null || block.text().isBlank()))
            throw new IllegalArgumentException("Текст блока обязателен");
        if (!textType && block.text() != null) throw new IllegalArgumentException("text разрешён только для текстовых блоков");
        if (block.type().equals("HEADING") ? block.level() == null : block.level() != null)
            throw new IllegalArgumentException("level обязателен только для HEADING");
        if (listType ? block.items() == null || block.items().isEmpty() : block.items() != null)
            throw new IllegalArgumentException("Непустой items обязателен только для списков");
        if (imageType ? block.mediaId() == null : block.mediaId() != null || block.alt() != null || block.caption() != null)
            throw new IllegalArgumentException("mediaId обязателен для IMAGE; alt и caption разрешены только для IMAGE");
    }

    private void checkVersion(Long current, Long requested) {
        if (requested == null) throw new IllegalArgumentException("Для обновления укажите version из последнего GET");
        if (!requested.equals(current)) throw new BlogConflictException("Запись уже изменена; загрузите свежие данные");
    }

    private static void validatePage(int page, int size) {
        if (page < 0 || page > 100000 || size < 1 || size > 100)
            throw new IllegalArgumentException("page: 0–100000, size: 1–100");
    }

    private BlogArticle findPublished(String slug) {
        return articles.findBySlugAndStatusAndPublishedAtLessThanEqual(slug, "PUBLISHED", Instant.now())
                .orElseThrow(() -> new NoSuchElementException("Опубликованная статья не найдена"));
    }
    private BlogArticle findArticle(Long id) {
        return articles.findById(id).orElseThrow(() -> new NoSuchElementException("Статья не найдена"));
    }
    private BlogCategory findCategory(Long id) {
        return categories.findById(id).orElseThrow(() -> new NoSuchElementException("Категория не найдена: " + id));
    }
    private MediaAsset findMedia(Long id) {
        return media.findByIdForUpdate(id).orElseThrow(() -> new NoSuchElementException("Изображение не найдено: " + id));
    }
    private CategoryResponse categoryResponse(BlogCategory category) {
        return new CategoryResponse(category.getId(), category.getVersion(), category.getName(), category.getSlug(), category.getSortOrder());
    }
    private Image image(MediaAsset asset) {
        return asset == null ? null : new Image(asset.getId(), "/api/media/" + asset.getId(), asset.getAltText());
    }
    private List<Card> cards(List<BlogCardRow> rows) {
        if (rows.isEmpty()) return List.of();
        Map<Long, List<CategoryResponse>> byArticle = new HashMap<>();
        for (BlogCategoryLink link : articles.findCategoryLinks(rows.stream().map(BlogCardRow::getId).toList()))
            byArticle.computeIfAbsent(link.getArticleId(), ignored -> new ArrayList<>()).add(categoryResponse(link.getCategory()));
        byArticle.values().forEach(values -> values.sort(Comparator.comparingInt(CategoryResponse::sortOrder)
                .thenComparing(CategoryResponse::id)));
        return rows.stream().map(row -> new Card(row.getId(), row.getVersion(), row.getTitle(), row.getSlug(),
                "/blog/" + row.getSlug(), row.getExcerpt(), row.getCoverMediaId() == null ? null
                : new Image(row.getCoverMediaId(), "/api/media/" + row.getCoverMediaId(), row.getCoverAlt()),
                List.copyOf(byArticle.getOrDefault(row.getId(), List.of())), row.getReadingMinutes(), row.getStatus(),
                row.getPublishedAt(), row.getCreatedAt(), row.getUpdatedAt())).toList();
    }

    private Card card(BlogArticle article) {
        return new Card(article.getId(), article.getVersion(), article.getTitle(), article.getSlug(), "/blog/" + article.getSlug(),
                article.getExcerpt(), image(article.getCover()), article.getCategories().stream()
                .sorted(Comparator.comparingInt(BlogCategory::getSortOrder).thenComparing(BlogCategory::getId))
                .map(this::categoryResponse).toList(), article.getReadingMinutes(), article.getStatus(),
                article.getPublishedAt(), article.getCreatedAt(), article.getUpdatedAt());
    }
    private ArticleResponse response(BlogArticle article) {
        List<Block> blocks;
        try { blocks = mapper.readValue(article.getBodyJson(), new TypeReference<List<Block>>() { }); }
        catch (JsonProcessingException error) { throw new IllegalStateException("Повреждено содержимое статьи " + article.getId(), error); }
        return new ArticleResponse(card(article), blocks, article.getImages().stream()
                .sorted(Comparator.comparing(MediaAsset::getId)).map(this::image).toList(),
                article.getSeoTitle() == null ? article.getTitle() : article.getSeoTitle(),
                article.getSeoDescription() == null ? article.getExcerpt() : article.getSeoDescription());
    }
    private static long wordCount(String text) { return text.isBlank() ? 0 : text.trim().split("\\s+").length; }
    private static String trimNullable(String value) { return value == null || value.isBlank() ? null : value.trim(); }
}
