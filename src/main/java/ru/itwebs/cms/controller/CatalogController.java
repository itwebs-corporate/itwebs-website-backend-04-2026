package ru.itwebs.cms.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;
import ru.itwebs.cms.catalog.CatalogService;
import java.util.List;
import static ru.itwebs.cms.catalog.CatalogDtos.*;

@RestController
@RequestMapping("/api/catalog/{kind}")
@Tag(name = "Каталоги", description = "kind: services, cases, specialists, team. Публичные списки и детальные записи; только опубликованные данные.")
public class CatalogController {
    private final CatalogService catalog;
    public CatalogController(CatalogService catalog) { this.catalog = catalog; }

    @GetMapping
    @Operation(summary = "Список записей", description = "category — slug категории; page от 0, size 1–100. Порядок sortOrder, id.")
    public EntryPage list(@PathVariable String kind, @RequestParam(required = false) String category,
                          @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return catalog.list(CatalogService.kind(kind), true, category, page, size);
    }
    @GetMapping("/categories")
    @Operation(summary = "Категории каталога")
    public List<CategoryView> categories(@PathVariable String kind) { return catalog.categories(CatalogService.kind(kind)); }
    @GetMapping("/{slug}")
    @Operation(summary = "Детальная запись по адресу", description = "Услуга включает FAQ, стек, процессы, задачи, аудиторию, сотрудничество и связанные кейсы. images содержит URL всех изображений содержимого.")
    public EntryView get(@PathVariable String kind, @PathVariable String slug) { return catalog.published(CatalogService.kind(kind), slug); }
}
