package ru.itwebs.cms.entity;

import jakarta.persistence.*;
import ru.itwebs.cms.catalog.CatalogDtos.Kind;
import ru.itwebs.cms.pages.PageDtos.DisplayOverride;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "page_collections", uniqueConstraints = @UniqueConstraint(columnNames = {"page_id", "collection_key"}))
public class PageCollection {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) public Long id;
    @Version public Long version;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) public SitePage page;
    @Column(name = "collection_key", nullable = false, length = 80) public String key;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) public Kind kind;
    @Column(nullable = false) public boolean allEntries;
    @ManyToMany
    @JoinTable(name = "page_collection_entries", joinColumns = @JoinColumn(name = "collection_id"), inverseJoinColumns = @JoinColumn(name = "entry_id"))
    @OrderColumn(name = "position") public List<CatalogEntry> entries = new ArrayList<>();
    @JdbcTypeCode(SqlTypes.JSON) @Column(columnDefinition = "jsonb", nullable = false)
    public List<DisplayOverride> overrides = new ArrayList<>();
}
