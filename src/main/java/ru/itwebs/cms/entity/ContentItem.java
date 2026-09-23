package ru.itwebs.cms.entity;


import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Entity
@Table(name = "content_items", indexes = @Index(name = "idx_content_section_order", columnList = "section_name,sort_order"))
public class ContentItem {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @NotBlank @Size(max = 80) @Column(name = "section_name", nullable = false, length = 80)
    private String sectionName;
    @NotBlank @Size(max = 80) @Column(name = "item_key", nullable = false, length = 80)
    private String itemKey;
    @Size(max = 80) @Column(name = "group_name", length = 80)
    private String groupName;
    @NotBlank @Size(max = 20) @Column(nullable = false, length = 20)
    private String type = "TEXT";
    @Size(max = 120) private String label;
    @Column(columnDefinition = "text") private String value;
    @Size(max = 1000) private String href;
    @ManyToOne(fetch = FetchType.EAGER) private MediaAsset media;
    @Column(name = "sort_order", nullable = false) private int sortOrder;
    private boolean published = true;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getSectionName() { return sectionName; }
    public void setSectionName(String sectionName) { this.sectionName = sectionName; }
    public String getItemKey() { return itemKey; }
    public void setItemKey(String itemKey) { this.itemKey = itemKey; }
    public String getGroupName() { return groupName; }
    public void setGroupName(String groupName) { this.groupName = groupName; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }
    public String getValue() { return value; }
    public void setValue(String value) { this.value = value; }
    public String getHref() { return href; }
    public void setHref(String href) { this.href = href; }
    public MediaAsset getMedia() { return media; }
    public void setMedia(MediaAsset media) { this.media = media; }
    public int getSortOrder() { return sortOrder; }
    public void setSortOrder(int sortOrder) { this.sortOrder = sortOrder; }
    public boolean isPublished() { return published; }
    public void setPublished(boolean published) { this.published = published; }
}
