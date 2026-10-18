package ru.itwebs.cms.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import ru.itwebs.cms.catalog.CatalogDtos.ServiceContent;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "service_offerings")
public class ServiceOffering extends CatalogEntry {
    @JdbcTypeCode(SqlTypes.JSON) @Column(columnDefinition = "jsonb") public ServiceContent body;
    @ManyToMany
    @JoinTable(name = "service_examples", joinColumns = @JoinColumn(name = "service_id"), inverseJoinColumns = @JoinColumn(name = "case_id"))
    @OrderColumn(name = "position") public List<CaseStudy> examples = new ArrayList<>();
}
