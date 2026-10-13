package ru.itwebs.cms.blog;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.Instant;
import java.util.List;
import java.util.Set;

public final class BlogDtos {
    private BlogDtos() { }

    @Schema(name = "BlogCategoryRequest")
    public record CategoryRequest(
            @NotBlank @Size(max = 80) String name,
            @NotBlank @Size(max = 80) @Pattern(regexp = "[a-z0-9]+(?:-[a-z0-9]+)*") String slug,
            @Min(0) int sortOrder,
            @Schema(description = "Не передавать при POST; при PUT обязательна текущая version из GET")
            @PositiveOrZero Long version) { }

    @Schema(name = "BlogCategoryResponse")
    public record CategoryResponse(Long id, Long version, String name, String slug, int sortOrder) { }

    @Schema(name = "BlogBlock", description = "Упорядоченный блок статьи. Текст — обычный текст, HTML не поддерживается. HEADING: text и level 2–4; PARAGRAPH/QUOTE: text; BULLET_LIST/NUMBERED_LIST: items; IMAGE: mediaId, необязательные alt и caption. Не относящиеся к типу поля должны отсутствовать.")
    public record Block(
            @NotBlank @Pattern(regexp = "HEADING|PARAGRAPH|QUOTE|BULLET_LIST|NUMBERED_LIST|IMAGE") String type,
            @Size(max = 20000) String text,
            @Min(2) @Max(4) Integer level,
            @Size(max = 100) List<@NotBlank @Size(max = 5000) String> items,
            @Positive Long mediaId,
            @Size(max = 255) String alt,
            @Size(max = 1000) String caption) { }

    @Schema(name = "BlogArticleRequest", example = """
            {"title":"Первая статья","slug":"first-article","excerpt":"Краткое описание",
             "blocks":[{"type":"HEADING","text":"Введение","level":2},
                       {"type":"PARAGRAPH","text":"Текст статьи"},
                       {"type":"BULLET_LIST","items":["Первый пункт","Второй пункт"]}],
             "status":"DRAFT","categoryIds":[],"publishedAt":null,"coverMediaId":null,
             "readingMinutes":null,"seoTitle":null,"seoDescription":null}
            """)
    public record ArticleRequest(
            @NotBlank @Size(max = 240) String title,
            @NotBlank @Size(max = 160) @Pattern(regexp = "[a-z0-9]+(?:-[a-z0-9]+)*") String slug,
            @Size(max = 2000) String excerpt,
            @NotNull @Size(max = 200) List<@NotNull @Valid Block> blocks,
            @NotBlank @Pattern(regexp = "DRAFT|PUBLISHED") String status,
            @Schema(description = "Для PUBLISHED: null означает публикацию сейчас; дата в будущем означает отложенную публикацию. При PUT null сохраняет прежнюю дату публикации.")
            Instant publishedAt,
            @Positive Long coverMediaId,
            @NotNull @Size(max = 20) Set<@NotNull @Positive Long> categoryIds,
            @Schema(description = "Необязательное переопределение; при null вычисляется из текста (200 слов/минуту)")
            @Min(1) @Max(1000) Integer readingMinutes,
            @Size(max = 240) String seoTitle,
            @Size(max = 500) String seoDescription,
            @Schema(description = "Не передавать при POST; при PUT обязательна текущая version из GET")
            @PositiveOrZero Long version) { }

    @Schema(name = "BlogImage")
    public record Image(Long id, String url, String alt) { }

    @Schema(name = "BlogArticleCard")
    public record Card(Long id, Long version, String title, String slug, String url, String excerpt,
                       Image cover, List<CategoryResponse> categories, int readingMinutes, String status,
                       Instant publishedAt, Instant createdAt, Instant updatedAt) { }

    @Schema(name = "BlogArticleResponse")
    public record ArticleResponse(Card article, List<Block> blocks, List<Image> images,
                                  String seoTitle, String seoDescription) { }

    @Schema(name = "BlogArticlePage")
    public record ArticlePage(List<Card> items, int page, int size, long totalElements, int totalPages) { }
}
