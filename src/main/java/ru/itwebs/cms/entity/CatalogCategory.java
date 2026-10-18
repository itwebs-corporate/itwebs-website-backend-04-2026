package ru.itwebs.cms.entity;

import jakarta.persistence.*;
import ru.itwebs.cms.catalog.CatalogDtos.Kind;

@Entity
@Table(name = "catalog_categories", uniqueConstraints = @UniqueConstraint(columnNames = {"kind", "slug"}))
public class CatalogCategory {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) public Long id;
    @Version public Long version;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) public Kind kind;
    @Column(nullable = false, length = 120) public String name;
    @Column(nullable = false, length = 80) public String slug;
    @Column(nullable = false) public int sortOrder;
}
