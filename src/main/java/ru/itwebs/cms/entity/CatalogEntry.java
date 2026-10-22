package ru.itwebs.cms.entity;

import jakarta.persistence.*;
import ru.itwebs.cms.catalog.CatalogDtos.Kind;
import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@Inheritance(strategy = InheritanceType.JOINED)
@Table(name = "catalog_entries", uniqueConstraints = @UniqueConstraint(columnNames = {"kind", "slug"}))
public abstract class CatalogEntry {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) public Long id;
    @Version public Long version;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) public Kind kind;
    @Column(nullable = false, length = 240) public String title;
    @Column(nullable = false, length = 160) public String slug;
    @Column(columnDefinition = "text") public String description;
    @Column(length = 240) public String price;
    @Column(length = 240) public String seoTitle;
    @Column(length = 1000) public String seoDescription;
    @Column(length = 1000) public String canonical;
    @Column(nullable = false) public boolean published;
    @Column(nullable = false) public int sortOrder;
    @ManyToOne(fetch = FetchType.LAZY) public MediaAsset cover;
    @ManyToOne(fetch = FetchType.LAZY) public MediaAsset mobileCover;
    @ManyToMany
    @JoinTable(name = "catalog_entry_categories", joinColumns = @JoinColumn(name = "entry_id"), inverseJoinColumns = @JoinColumn(name = "category_id"))
    public Set<CatalogCategory> categories = new LinkedHashSet<>();
    @ManyToMany
    @JoinTable(name = "catalog_entry_images", joinColumns = @JoinColumn(name = "entry_id"), inverseJoinColumns = @JoinColumn(name = "media_id"))
    public Set<MediaAsset> images = new LinkedHashSet<>();
}
