package ru.itwebs.cms.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import ru.itwebs.cms.catalog.CatalogDtos.MemberContent;

@Entity
@Table(name = "team_members")
public class TeamMember extends CatalogEntry {
    @JdbcTypeCode(SqlTypes.JSON) @Column(columnDefinition = "jsonb") public MemberContent body;
}
