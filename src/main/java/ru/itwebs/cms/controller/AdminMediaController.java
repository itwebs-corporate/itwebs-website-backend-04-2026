package ru.itwebs.cms.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import ru.itwebs.cms.dto.MediaResponse;
import ru.itwebs.cms.dto.MediaUpdateRequest;
import ru.itwebs.cms.service.MediaService;
import java.io.IOException;
import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/admin/media")
@Tag(name = "Админ: изображения", description = "Загрузка, просмотр, изменение описания и удаление файлов")
@SecurityRequirement(name = "basicAuth")
public class AdminMediaController {
    private final MediaService media;
    public AdminMediaController(MediaService media) { this.media = media; }

    @GetMapping
    @Operation(summary = "Список изображений")
    public List<MediaResponse> list() { return media.list().stream().map(MediaResponse::from).toList(); }

    @GetMapping("/{id}")
    @Operation(summary = "Данные изображения по ID")
    public MediaResponse get(@PathVariable Long id) { return MediaResponse.from(media.find(id)); }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Загрузить изображение", description = "JPEG, PNG, WebP, GIF или AVIF, до 15 МБ.")
    public ResponseEntity<MediaResponse> upload(@RequestPart("file") MultipartFile file,
                                                 @RequestParam String altText) throws IOException {
        MediaResponse result = MediaResponse.from(media.upload(file, altText));
        return ResponseEntity.created(URI.create("/api/admin/media/" + result.id())).body(result);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Изменить описание изображения")
    public MediaResponse update(@PathVariable Long id, @Valid @RequestBody MediaUpdateRequest request) {
        media.updateAlt(id, request.altText());
        return MediaResponse.from(media.find(id));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Удалить изображение", description = "Файл нельзя удалить, пока он используется элементом контента.")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        media.delete(id);
        return ResponseEntity.noContent().build();
    }
}
