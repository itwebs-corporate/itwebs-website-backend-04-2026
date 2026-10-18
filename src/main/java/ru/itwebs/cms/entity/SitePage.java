package ru.itwebs.cms.entity;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "site_pages")
public class SitePage {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) public Long id;
    @Version public Long version;
    @Column(nullable = false, unique = true, length = 80) public String slug;
    @Column(nullable = false, unique = true, length = 240) public String path;
    @Column(nullable = false, length = 240) public String title;
    @Column(length = 240) public String seoTitle;
    @Column(length = 1000) public String seoDescription;
    @Column(length = 1000) public String canonical;
    @Column(nullable = false) public boolean published = true;
    @OneToMany(mappedBy = "page", cascade = CascadeType.ALL)
    @OrderBy("sortOrder ASC, id ASC") public List<SiteBlock> blocks = new ArrayList<>();
    @OneToMany(mappedBy = "page", cascade = CascadeType.ALL)
    @OrderBy("id ASC") public List<PageCollection> collections = new ArrayList<>();
    @ManyToMany
    @JoinTable(name = "page_reused_blocks", joinColumns = @JoinColumn(name = "page_id"), inverseJoinColumns = @JoinColumn(name = "block_id"))
    @OrderColumn(name = "position") public List<SiteBlock> reusedBlocks = new ArrayList<>();
    public String getSlug() { return slug; }
}
