package ru.itwebs.cms.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.http.ProblemDetail;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;
import ru.itwebs.cms.blog.BlogService;
import static ru.itwebs.cms.blog.BlogDtos.*;
import java.util.List;

@RestController
@RequestMapping("/api/blog")
@Tag(name = "Блог", description = "Публичная выдача статей: только PUBLISHED с наступившей publishedAt. Тексты страницы: GET /api/pages/blog; общие блоки статьи: GET /api/pages/blog-article.")
@ApiResponses({
        @ApiResponse(responseCode = "400", description = "Некорректные параметры", content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class))),
        @ApiResponse(responseCode = "404", description = "Опубликованная статья не найдена", content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))})
public class PublicBlogController {
    private final BlogService blog;
    public PublicBlogController(BlogService blog) { this.blog = blog; }

    @GetMapping("/categories")
    @ApiResponse(responseCode = "200", description = "Категории", content = @Content(array = @io.swagger.v3.oas.annotations.media.ArraySchema(schema = @Schema(implementation = CategoryResponse.class))))
    @Operation(summary = "Категории для фильтров и меток", description = "sortOrder ASC, id ASC; включая категории без статей. Кнопка Все берётся из blog-page/allLabel; для неё не передавайте category.")
    public List<CategoryResponse> categories() { return blog.categories(); }

    @GetMapping("/articles")
    @ApiResponse(responseCode = "200", description = "Страница статей", content = @Content(schema = @Schema(implementation = ArticlePage.class)))
    @Operation(summary = "Опубликованные статьи с пагинацией", description = "page от 0 до 100000, size от 1 до 100. category — необязательный slug категории. Сортировка publishedAt DESC, id DESC. items содержит карточки без тела статьи; неизвестная категория даёт пустой список.")
    public ArticlePage list(@RequestParam(defaultValue = "0") int page,
                            @RequestParam(defaultValue = "20") int size,
                            @RequestParam(required = false) String category) {
        return blog.list(true, page, size, category, null);
    }

    @GetMapping("/articles/{slug}")
    @ApiResponse(responseCode = "200", description = "Статья", content = @Content(schema = @Schema(implementation = ArticleResponse.class)))
    @Operation(summary = "Опубликованная статья по slug", description = "Возвращает карточку, упорядоченные blocks, изображения с URL и alt и SEO-метаданные. Текст блоков выводите как текст, а не HTML. Для HEADING якорь можно построить из индекса блока. У IMAGE alt блока переопределяет alt из images. SEO по умолчанию берётся из title/excerpt. Черновики и будущие публикации возвращают 404.")
    public ArticleResponse get(@PathVariable String slug) { return blog.published(slug); }

    @GetMapping("/articles/{slug}/related")
    @ApiResponse(responseCode = "200", description = "Рекомендации", content = @Content(array = @io.swagger.v3.oas.annotations.media.ArraySchema(schema = @Schema(implementation = Card.class))))
    @Operation(summary = "Карточки для блока «Вам может быть интересно»", description = "Последние опубликованные статьи без текущей, publishedAt DESC, id DESC. limit 1–12. Возвращается меньше карточек, если статей недостаточно.")
    public List<Card> related(@PathVariable String slug, @RequestParam(defaultValue = "3") int limit) {
        return blog.related(slug, limit);
    }
}
