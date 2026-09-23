package ru.itwebs.cms.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.itwebs.cms.dto.ContentRequest;
import ru.itwebs.cms.dto.ContentResponse;
import ru.itwebs.cms.service.ContentService;
import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/admin/content")
@Tag(name = "Админ: контент", description = "CRUD всех надписей, ссылок и мест для изображений")
@SecurityRequirement(name = "basicAuth")
public class AdminContentController {
    private final ContentService content;
    public AdminContentController(ContentService content) { this.content = content; }

    @GetMapping
    @Operation(summary = "Все элементы контента", description = "Включает скрытые элементы.")
    public List<ContentResponse> list() { return content.list().stream().map(ContentResponse::from).toList(); }

    @GetMapping("/{id}")
    @Operation(summary = "Элемент контента по ID")
    public ContentResponse get(@PathVariable Long id) { return ContentResponse.from(content.find(id)); }

    @PostMapping
    @Operation(summary = "Создать элемент контента")
    public ResponseEntity<ContentResponse> create(@Valid @RequestBody ContentRequest request) {
        ContentResponse result = ContentResponse.from(content.create(request));
        return ResponseEntity.created(URI.create("/api/admin/content/" + result.id())).body(result);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Обновить элемент контента")
    public ContentResponse update(@PathVariable Long id, @Valid @RequestBody ContentRequest request) {
        return ContentResponse.from(content.update(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Удалить элемент контента")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        content.delete(id);
        return ResponseEntity.noContent().build();
    }
}
