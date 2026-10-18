package ru.itwebs.cms.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import ru.itwebs.cms.catalog.ServiceComponentDtos.ComponentOverride;

@Entity
@Table(name = "service_component_links", indexes = {
        @Index(name = "idx_service_component_link_service", columnList = "service_id,position"),
        @Index(name = "idx_service_component_link_component", columnList = "component_id")})
public class ServiceComponentLink {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) public Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "service_id") public ServiceOffering service;
    @ManyToOne(optional = false) @JoinColumn(name = "component_id") public ServiceComponent component;
    @Column(nullable = false) public int position;
    @JdbcTypeCode(SqlTypes.JSON) @Column(name = "content_override", columnDefinition = "jsonb") public ComponentOverride override;
}
