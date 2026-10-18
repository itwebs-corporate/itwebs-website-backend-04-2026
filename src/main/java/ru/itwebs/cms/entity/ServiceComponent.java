package ru.itwebs.cms.entity;

import jakarta.persistence.*;
import ru.itwebs.cms.catalog.ServiceComponentDtos.ComponentKind;

@Entity
@Table(name = "service_components", indexes = @Index(name = "idx_service_component_kind_order", columnList = "kind,sort_order"))
public class ServiceComponent {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) public Long id;
    @Version public Long version;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) public ComponentKind kind;
    @Column(nullable = false, length = 1000) public String title;
    @Column(columnDefinition = "text") public String description;
    @Column(length = 240) public String price;
    @ManyToOne public MediaAsset image;
    @Column(name = "sort_order", nullable = false) public int sortOrder;
    @Column(nullable = false, unique = true, length = 64) public String fingerprint;
}
