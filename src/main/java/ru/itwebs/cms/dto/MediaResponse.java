package ru.itwebs.cms.dto;

import ru.itwebs.cms.entity.MediaAsset;

public record MediaResponse(Long id, String originalName, String contentType,
                            String altText, long sizeBytes, String url) {
    public static MediaResponse from(MediaAsset asset) {
        return new MediaResponse(asset.getId(), asset.getOriginalName(), asset.getContentType(),
                asset.getAltText(), asset.getSizeBytes(), "/api/media/" + asset.getId());
    }
}
