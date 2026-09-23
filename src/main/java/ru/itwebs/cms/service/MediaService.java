package ru.itwebs.cms.service;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import ru.itwebs.cms.entity.MediaAsset;
import ru.itwebs.cms.repository.ContentRepository;
import ru.itwebs.cms.repository.MediaRepository;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class MediaService {
    private static final Set<String> TYPES = Set.of("image/jpeg", "image/png", "image/webp", "image/gif", "image/avif");
    private final MediaRepository media;
    private final ContentRepository content;
    private final StorageService storage;
    public MediaService(MediaRepository media, ContentRepository content, StorageService storage) {
        this.media = media; this.content = content; this.storage = storage;
    }
    public List<MediaAsset> list() { return media.findAll(); }
    public long count() { return media.count(); }
    public MediaAsset find(Long id) { return media.findById(id).orElseThrow(); }
    public MediaAsset upload(MultipartFile file, String altText) throws IOException {
        String type = file.getContentType();
        if (file.isEmpty() || type == null || !TYPES.contains(type)) throw new IllegalArgumentException("Разрешены JPEG, PNG, WebP, GIF и AVIF");
        if (altText == null || altText.isBlank()) throw new IllegalArgumentException("Укажите описание изображения");
        String suffix = switch (type) { case "image/jpeg" -> ".jpg"; case "image/png" -> ".png"; case "image/webp" -> ".webp"; case "image/gif" -> ".gif"; default -> ".avif"; };
        String key = UUID.randomUUID() + suffix;
        storage.upload(file, key, type);
        MediaAsset asset = new MediaAsset();
        asset.setObjectKey(key);
        String originalName = file.getOriginalFilename();
        asset.setOriginalName(originalName == null || originalName.isBlank() ? "image" : originalName.substring(0, Math.min(originalName.length(), 255)));
        asset.setContentType(type);
        asset.setAltText(altText.trim());
        asset.setSizeBytes(file.getSize());
        try { return media.save(asset); }
        catch (RuntimeException e) { storage.delete(asset); throw e; }
    }
    public ResponseBytes<GetObjectResponse> download(MediaAsset asset) { return storage.download(asset); }
    public void updateAlt(Long id, String altText) {
        if (altText == null || altText.isBlank() || altText.length() > 255) throw new IllegalArgumentException("Укажите описание до 255 символов");
        MediaAsset asset = find(id);
        asset.setAltText(altText.trim());
        media.save(asset);
    }
    public void delete(Long id) {
        if (content.countByMediaId(id) > 0) throw new IllegalArgumentException("Изображение используется на странице");
        MediaAsset asset = find(id);
        storage.delete(asset);
        media.delete(asset);
    }
}
