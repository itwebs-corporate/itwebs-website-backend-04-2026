package ru.itwebs.cms.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@Table(name = "blog_articles", uniqueConstraints = @UniqueConstraint(name = "uk_blog_article_slug", columnNames = "slug"), indexes = @Index(name = "idx_blog_publication", columnList = "status,published_at,id"))
public class BlogArticle {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Version
    private Long version;
    @Column(nullable = false, length = 240)
    private String title;
    @Column(nullable = false, length = 160)
    private String slug;
    @Column(nullable = false, length = 2000)
    private String excerpt = "";
    @Column(name = "body_json", nullable = false, columnDefinition = "text")
    private String bodyJson = "[]";
    @Column(nullable = false, length = 20)
    private String status = "DRAFT";
    @Column(name = "published_at")
    private Instant publishedAt;
    @Column(nullable = false)
    private int readingMinutes = 1;
    @Column(length = 240)
    private String seoTitle;
    @Column(length = 500)
    private String seoDescription;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "cover_id")
    private MediaAsset cover;
    @org.hibernate.annotations.BatchSize(size = 100)
    @ManyToMany
    @JoinTable(name = "blog_article_categories", joinColumns = @JoinColumn(name = "article_id"), inverseJoinColumns = @JoinColumn(name = "category_id"),
            indexes = @Index(name = "idx_blog_category_articles", columnList = "category_id,article_id"))
    private Set<BlogCategory> categories = new LinkedHashSet<>();
    @org.hibernate.annotations.BatchSize(size = 100)
    @ManyToMany
    @JoinTable(name = "blog_article_images", joinColumns = @JoinColumn(name = "article_id"), inverseJoinColumns = @JoinColumn(name = "media_id"),
            indexes = @Index(name = "idx_blog_media_articles", columnList = "media_id,article_id"))
    private Set<MediaAsset> images = new LinkedHashSet<>();
    @Column(nullable = false, updatable = false)
    private Instant createdAt;
    @Column(nullable = false)
    private Instant updatedAt;
    @PrePersist
    void onCreate() { createdAt = Instant.now(); updatedAt = createdAt; }
    @PreUpdate
    void onUpdate() { updatedAt = Instant.now(); }
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getSlug() { return slug; }
    public void setSlug(String slug) { this.slug = slug; }
    public String getExcerpt() { return excerpt; }
    public void setExcerpt(String excerpt) { this.excerpt = excerpt; }
    public String getBodyJson() { return bodyJson; }
    public void setBodyJson(String bodyJson) { this.bodyJson = bodyJson; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Instant getPublishedAt() { return publishedAt; }
    public void setPublishedAt(Instant publishedAt) { this.publishedAt = publishedAt; }
    public int getReadingMinutes() { return readingMinutes; }
    public void setReadingMinutes(int readingMinutes) { this.readingMinutes = readingMinutes; }
    public String getSeoTitle() { return seoTitle; }
    public void setSeoTitle(String seoTitle) { this.seoTitle = seoTitle; }
    public String getSeoDescription() { return seoDescription; }
    public void setSeoDescription(String seoDescription) { this.seoDescription = seoDescription; }
    public MediaAsset getCover() { return cover; }
    public void setCover(MediaAsset cover) { this.cover = cover; }
    public Set<BlogCategory> getCategories() { return categories; }
    public void setCategories(Set<BlogCategory> categories) { this.categories = categories; }
    public Set<MediaAsset> getImages() { return images; }
    public void setImages(Set<MediaAsset> images) { this.images = images; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
