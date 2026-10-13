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
import java.util.List;

@RestController
@RequestMapping("/api/admin/blog/categories")
@Tag(name = "Админ: категории блога", description = "Категории используются в фильтрах и метках статей; порядок задаёт sortOrder. Slug all зарезервирован.")
@SecurityRequirement(name = "basicAuth")
@ApiResponses({
        @ApiResponse(responseCode = "400", description = "Ошибка валидации", content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class))),
        @ApiResponse(responseCode = "401", description = "Требуется HTTP Basic"),
        @ApiResponse(responseCode = "403", description = "Требуется роль ADMIN"),
        @ApiResponse(responseCode = "404", description = "Категория не найдена", content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class))),
        @ApiResponse(responseCode = "409", description = "Slug занят, версия устарела или категория используется", content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))})
public class AdminBlogCategoryController {
    private final BlogService blog;
    public AdminBlogCategoryController(BlogService blog) { this.blog = blog; }

    @GetMapping
    @ApiResponse(responseCode = "200", description = "Категории", content = @Content(array = @io.swagger.v3.oas.annotations.media.ArraySchema(schema = @Schema(implementation = CategoryResponse.class))))
    @Operation(summary = "Категории в порядке sortOrder ASC, id ASC")
    public List<CategoryResponse> list() { return blog.categories(); }

    @GetMapping("/{id}")
    @ApiResponse(responseCode = "200", description = "Категория", content = @Content(schema = @Schema(implementation = CategoryResponse.class)))
    @Operation(summary = "Получить категорию")
    public CategoryResponse get(@PathVariable Long id) { return blog.category(id); }

    @PostMapping
    @Operation(summary = "Создать категорию")
    @ApiResponse(responseCode = "201", description = "Категория создана", content = @Content(schema = @Schema(implementation = CategoryResponse.class)))
    public ResponseEntity<CategoryResponse> create(@Valid @RequestBody CategoryRequest request) {
        CategoryResponse response = blog.createCategory(request);
        return ResponseEntity.created(URI.create("/api/admin/blog/categories/" + response.id())).body(response);
    }

    @PutMapping("/{id}")
    @ApiResponse(responseCode = "200", description = "Обновлённая категория", content = @Content(schema = @Schema(implementation = CategoryResponse.class)))
    @Operation(summary = "Обновить категорию", description = "version обязательна: передайте значение последнего GET.")
    public CategoryResponse update(@PathVariable Long id, @Valid @RequestBody CategoryRequest request) {
        return blog.updateCategory(id, request);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Удалить неиспользуемую категорию", description = "Если категория связана хотя бы с одной статьёй, включая черновики, возвращается 409. Сначала удалите связи через PUT статей.")
    @ApiResponse(responseCode = "204", description = "Категория удалена", content = @Content)
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        blog.deleteCategory(id); return ResponseEntity.noContent().build();
    }
}
