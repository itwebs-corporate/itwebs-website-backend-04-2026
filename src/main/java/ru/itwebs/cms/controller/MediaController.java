package ru.itwebs.cms.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.itwebs.cms.entity.MediaAsset;
import ru.itwebs.cms.service.MediaService;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/media")
@Tag(name = "Публичные изображения", description = "Файлы из S3-совместимого хранилища")
public class MediaController {
    private final MediaService media;
    public MediaController(MediaService media) { this.media = media; }

    @GetMapping("/{id}")
    @Operation(summary = "Скачать изображение")
    @ApiResponse(responseCode = "200", description = "Изображение", content = @Content(mediaType = "image/*"))
    public ResponseEntity<byte[]> get(@PathVariable Long id) {
        MediaAsset asset = media.find(id);
        return ResponseEntity.ok().contentType(MediaType.parseMediaType(asset.getContentType()))
                .cacheControl(CacheControl.maxAge(1, TimeUnit.DAYS).cachePublic())
                .body(media.download(asset).asByteArray());
    }
}
