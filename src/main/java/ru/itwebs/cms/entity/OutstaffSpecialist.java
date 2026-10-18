package ru.itwebs.cms.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import ru.itwebs.cms.catalog.CatalogDtos.SpecialistContent;

@Entity
@Table(name = "outstaff_specialists")
public class OutstaffSpecialist extends CatalogEntry {
    @JdbcTypeCode(SqlTypes.JSON) @Column(columnDefinition = "jsonb") public SpecialistContent body;
}
