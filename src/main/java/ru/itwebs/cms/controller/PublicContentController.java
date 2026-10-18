package ru.itwebs.cms.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;
import ru.itwebs.cms.dto.ContentResponse;
import ru.itwebs.cms.service.ContentService;
import java.util.List;

@RestController
@RequestMapping("/api/content")
@Tag(name = "Архив публичного контента", description = "Устаревший снимок до миграции. Для актуальных данных используйте /api/pages и /api/catalog.")
public class PublicContentController {
    private final ContentService content;
    public PublicContentController(ContentService content) { this.content = content; }

    @GetMapping
    @Operation(summary = "Список опубликованных элементов", description = "Возвращает элементы в порядке отображения; section фильтрует по разделу.")
    public List<ContentResponse> list(@RequestParam(required = false) String section) {
        return content.published(section).stream().map(ContentResponse::from).toList();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Опубликованный элемент по ID")
    public ContentResponse get(@PathVariable Long id) { return ContentResponse.from(content.findPublished(id)); }
}
