package ru.itwebs.cms.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import ru.itwebs.cms.pages.PageDtos.*;
import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@Table(name = "site_blocks", uniqueConstraints = @UniqueConstraint(columnNames = {"page_id", "block_key"}))
public class SiteBlock {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) public Long id;
    @Version public Long version;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) public SitePage page;
    @Column(name = "block_key", nullable = false, length = 80) public String key;
    @Column(nullable = false, length = 240) public String title;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) public BlockType type;
    @Column(nullable = false) public int sortOrder;
    @Column(nullable = false) public boolean published = true;
    @JdbcTypeCode(SqlTypes.JSON) @Column(columnDefinition = "jsonb", nullable = false) public BlockSchema schema;
    @JdbcTypeCode(SqlTypes.JSON) @Column(columnDefinition = "jsonb", nullable = false) public BlockData data;
    @ManyToMany
    @JoinTable(name = "site_block_images", joinColumns = @JoinColumn(name = "block_id"), inverseJoinColumns = @JoinColumn(name = "media_id"))
    public Set<MediaAsset> images = new LinkedHashSet<>();
}
