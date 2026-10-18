package ru.itwebs.cms.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.itwebs.cms.catalog.CatalogService;
import java.net.URI;
import java.util.List;
import static ru.itwebs.cms.catalog.CatalogDtos.*;

@RestController
@RequestMapping("/api/admin/catalog/{kind}")
@Tag(name = "Админ: каталоги", description = "kind: services, cases, specialists, team. Запись сохраняется целиком. При PUT обязательна version из entry.version. Передайте ровно одно поле service/project/specialist/member согласно каталогу; остальные null. categoryIds — категории этого каталога, caseIds — упорядоченные ID кейсов только для услуги.")
@SecurityRequirement(name = "basicAuth")
public class AdminCatalogController {
    private final CatalogService catalog;
    public AdminCatalogController(CatalogService catalog) { this.catalog = catalog; }
    @GetMapping
    @Operation(summary = "Записи включая черновики")
    public EntryPage list(@PathVariable String kind, @RequestParam(required = false) String category,
                          @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return catalog.list(CatalogService.kind(kind), false, category, page, size);
    }
    @GetMapping("/{id}")
    @Operation(summary = "Открыть запись для редактирования")
    public EntryView get(@PathVariable String kind, @PathVariable Long id) { return catalog.get(CatalogService.kind(kind), id); }
    @PostMapping
    @Operation(summary = "Создать запись")
    public ResponseEntity<EntryView> create(@PathVariable String kind, @Valid @RequestBody EntryRequest request) {
        EntryView result = catalog.save(CatalogService.kind(kind), null, request);
        return ResponseEntity.created(URI.create("/api/admin/catalog/" + kind + "/" + result.entry().id())).body(result);
    }
    @PutMapping("/{id}")
    @Operation(summary = "Сохранить запись со всеми блоками")
    public EntryView save(@PathVariable String kind, @PathVariable Long id, @Valid @RequestBody EntryRequest request) {
        return catalog.save(CatalogService.kind(kind), id, request);
    }
    @DeleteMapping("/{id}")
    @Operation(summary = "Удалить запись", description = "Используемые кейсы и записи, привязанные к странице, защищены внешними ключами. Для скрытия установите published=false.")
    public ResponseEntity<Void> delete(@PathVariable String kind, @PathVariable Long id) {
        catalog.delete(CatalogService.kind(kind), id);
        return ResponseEntity.noContent().build();
    }
    @GetMapping("/categories")
    @Operation(summary = "Категории")
    public List<CategoryView> categories(@PathVariable String kind) { return catalog.categories(CatalogService.kind(kind)); }
    @GetMapping("/categories/{id}")
    @Operation(summary = "Категория для редактирования")
    public CategoryView category(@PathVariable String kind, @PathVariable Long id) { return catalog.category(CatalogService.kind(kind), id); }
    @PostMapping("/categories")
    @Operation(summary = "Создать категорию")
    public ResponseEntity<CategoryView> createCategory(@PathVariable String kind, @Valid @RequestBody CategoryRequest request) {
        CategoryView result = catalog.saveCategory(CatalogService.kind(kind), null, request);
        return ResponseEntity.created(URI.create("/api/admin/catalog/" + kind + "/categories/" + result.id())).body(result);
    }
    @PutMapping("/categories/{id}")
    @Operation(summary = "Сохранить категорию")
    public CategoryView saveCategory(@PathVariable String kind, @PathVariable Long id, @Valid @RequestBody CategoryRequest request) {
        return catalog.saveCategory(CatalogService.kind(kind), id, request);
    }
    @DeleteMapping("/categories/{id}")
    @Operation(summary = "Удалить неиспользуемую категорию")
    public ResponseEntity<Void> deleteCategory(@PathVariable String kind, @PathVariable Long id) {
        catalog.deleteCategory(CatalogService.kind(kind), id);
        return ResponseEntity.noContent().build();
    }
}
