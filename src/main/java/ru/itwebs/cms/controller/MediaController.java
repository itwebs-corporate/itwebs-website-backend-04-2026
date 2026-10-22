package ru.itwebs.cms.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ContentDisposition;
import java.nio.charset.StandardCharsets;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.itwebs.cms.entity.MediaAsset;
import ru.itwebs.cms.service.MediaService;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/media")
@Tag(name = "Публичные файлы", description = "Файлы из S3-совместимого хранилища")
public class MediaController {
    private final MediaService media;
    public MediaController(MediaService media) { this.media = media; }

    @GetMapping("/{id}")
    @Operation(summary = "Получить изображение или скачать документ")
    @ApiResponse(responseCode = "200", description = "Изображение или документ", content = {
            @Content(mediaType = "image/*"), @Content(mediaType = "application/pdf"),
            @Content(mediaType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")})
    public ResponseEntity<byte[]> get(@PathVariable Long id) {
        MediaAsset asset = media.find(id);
        boolean image = asset.getContentType().startsWith("image/");
        String filename = asset.getOriginalName().replaceAll("[\\p{Cntrl}/\\\\]", "_");
        return ResponseEntity.ok().contentType(MediaType.parseMediaType(asset.getContentType()))
                .header("X-Content-Type-Options", "nosniff")
                .header("Content-Security-Policy", "default-src 'none'; img-src data:; style-src 'unsafe-inline'; sandbox")
                .header(HttpHeaders.CONTENT_DISPOSITION, (image ? ContentDisposition.inline() : ContentDisposition.attachment())
                        .filename(filename, StandardCharsets.UTF_8).build().toString())
                .cacheControl(CacheControl.maxAge(1, TimeUnit.DAYS).cachePublic())
                .body(media.download(asset).asByteArray());
    }
}
