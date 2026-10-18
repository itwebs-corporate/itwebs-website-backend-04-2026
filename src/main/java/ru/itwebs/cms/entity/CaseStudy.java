package ru.itwebs.cms.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import ru.itwebs.cms.catalog.CatalogDtos.CaseContent;

@Entity
@Table(name = "case_studies")
public class CaseStudy extends CatalogEntry {
    @JdbcTypeCode(SqlTypes.JSON) @Column(columnDefinition = "jsonb") public CaseContent body;
}
