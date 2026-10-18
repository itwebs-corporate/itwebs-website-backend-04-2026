package ru.itwebs.cms.pages;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.List;
import java.util.Map;

public final class PageDtos {
    private PageDtos() { }
    public enum BlockType { HERO, COPY, CARDS, NAVIGATION, FORM, SEO, FAQ }
    public record FieldRule(String label, String type) { }
    public record BlockSchema(Map<String, FieldRule> fields, Map<String, FieldRule> itemFields) { }
    public record Value(@Size(max = 50000) String text, @Size(max = 1000) String href, @Positive Long mediaId,
                        Boolean published) {
        public Value(String text, String href, Long mediaId) { this(text, href, mediaId, true); }
        public boolean visible() { return !Boolean.FALSE.equals(published); }
    }
    public record BlockData(@NotNull @Size(max = 200) Map<@NotBlank String, @NotNull @Valid Value> fields,
                            @NotNull @Size(max = 200) List<@NotNull Map<@NotBlank String, @NotNull @Valid Value>> items) { }
    public record BlockRequest(@NotNull @Valid BlockData data, boolean published, @NotNull @PositiveOrZero Long version) { }
    public record PageRequest(@NotBlank @Size(max = 240) String title, @Size(max = 240) String seoTitle,
                              @Size(max = 1000) String seoDescription, @Size(max = 1000) String canonical,
                              boolean published, @NotNull @PositiveOrZero Long version) { }
    public record DisplayOverride(@NotNull @Positive Long entryId, @Size(max = 240) String title,
                                   @Size(max = 50000) String description, @Size(max = 240) String position,
                                   @Size(max = 255) String imageAlt, @Valid ru.itwebs.cms.catalog.CatalogDtos.SpecialistContent specialist) {
        public DisplayOverride(Long entryId, String title, String description, String position, String imageAlt) {
            this(entryId, title, description, position, imageAlt, null);
        }
    }
    public record CollectionRequest(boolean allEntries, @NotNull @Size(max = 100) List<@NotNull @Positive Long> entryIds,
                                    @Size(max = 100) List<@NotNull @Valid DisplayOverride> overrides,
                                    @NotNull @PositiveOrZero Long version) { }
    public record PageCard(Long id, Long version, String slug, String path, String title, boolean published) { }
    public record BlockView(Long id, Long version, String key, String title, BlockType type, String ownerPage, boolean shared,
                            boolean published, BlockSchema schema, BlockData data) { }
    public record CollectionView(Long id, Long version, String key, ru.itwebs.cms.catalog.CatalogDtos.Kind kind,
                                 boolean allEntries, List<Long> entryIds, List<DisplayOverride> overrides) { }
    public record AdminPage(PageCard page, String seoTitle, String seoDescription, String canonical,
                            List<BlockView> blocks, List<CollectionView> collections) { }
}
