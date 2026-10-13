package ru.itwebs.cms.repository;

import java.time.Instant;

/** Scalar projection: list queries deliberately omit the large article body. */
public interface BlogCardRow {
    Long getId();
    Long getVersion();
    String getTitle();
    String getSlug();
    String getExcerpt();
    Long getCoverMediaId();
    String getCoverAlt();
    int getReadingMinutes();
    String getStatus();
    Instant getPublishedAt();
    Instant getCreatedAt();
    Instant getUpdatedAt();
}
