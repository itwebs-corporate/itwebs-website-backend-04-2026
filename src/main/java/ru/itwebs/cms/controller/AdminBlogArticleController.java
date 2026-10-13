package ru.itwebs.cms.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.http.ProblemDetail;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.itwebs.cms.blog.BlogService;
import static ru.itwebs.cms.blog.BlogDtos.*;
import java.net.URI;

@RestController
@RequestMapping("/api/admin/blog/articles")
@Tag(name = "Админ: статьи блога", description = "CRUD статей. PUT полностью заменяет редактируемые поля; version защищает от перезаписи чужих изменений. Изображения загружаются через POST /api/admin/media.")
@SecurityRequirement(name = "basicAuth")
@ApiResponses({
        @ApiResponse(responseCode = "400", description = "Ошибка валидации, пагинации или структуры блоков", content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class))),
        @ApiResponse(responseCode = "401", description = "Требуется HTTP Basic"),
        @ApiResponse(responseCode = "403", description = "Требуется роль ADMIN"),
        @ApiResponse(responseCode = "404", description = "Статья, категория или изображение не найдены", content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class))),
        @ApiResponse(responseCode = "409", description = "Slug занят, версия устарела или конфликт связей", content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))})
public class AdminBlogArticleController {
    private final BlogService blog;
    public AdminBlogArticleController(BlogService blog) { this.blog = blog; }

    @GetMapping
    @ApiResponse(responseCode = "200", description = "Страница статей", content = @Content(schema = @Schema(implementation = ArticlePage.class)))
    @Operation(summary = "Список статей, включая черновики и отложенные публикации", description = "page начинается с 0; size 1–100. category — slug категории, status — DRAFT или PUBLISHED. Сортировка: updatedAt DESC, id DESC.")
    public ArticlePage list(@RequestParam(defaultValue = "0") int page,
                            @RequestParam(defaultValue = "20") int size,
                            @RequestParam(required = false) String category,
                            @RequestParam(required = false) String status) {
        return blog.list(false, page, size, category, status);
    }

    @GetMapping("/{id}")
    @ApiResponse(responseCode = "200", description = "Статья", content = @Content(schema = @Schema(implementation = ArticleResponse.class)))
    @Operation(summary = "Получить статью для редактирования")
    public ArticleResponse get(@PathVariable Long id) { return blog.article(id); }

    @PostMapping
    @Operation(summary = "Создать статью", description = "DRAFT допускает пустые blocks, categoryIds и отсутствие обложки/описания. PUBLISHED требует непустые blocks, categoryIds, excerpt и coverMediaId. publishedAt в будущем откладывает публичную выдачу.")
    @ApiResponse(responseCode = "201", description = "Статья создана; Location содержит адрес административной записи", content = @Content(schema = @Schema(implementation = ArticleResponse.class)))
    public ResponseEntity<ArticleResponse> create(@Valid @RequestBody ArticleRequest request) {
        ArticleResponse response = blog.create(request);
        return ResponseEntity.created(URI.create("/api/admin/blog/articles/" + response.article().id())).body(response);
    }

    @PutMapping("/{id}")
    @ApiResponse(responseCode = "200", description = "Обновлённая статья", content = @Content(schema = @Schema(implementation = ArticleResponse.class)))
    @Operation(summary = "Обновить статью целиком", description = "Передайте version из article.version последнего GET. DRAFT снимает статью с публикации. publishedAt=null сохраняет прежнюю дату, а при первой публикации устанавливает текущую.")
    public ArticleResponse update(@PathVariable Long id, @Valid @RequestBody ArticleRequest request) {
        return blog.update(id, request);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Удалить статью", description = "Удаление окончательное. Файлы в медиахранилище сохраняются. Для временного скрытия используйте PUT со status=DRAFT.")
    @ApiResponse(responseCode = "204", description = "Статья удалена", content = @Content)
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        blog.delete(id); return ResponseEntity.noContent().build();
    }
}
