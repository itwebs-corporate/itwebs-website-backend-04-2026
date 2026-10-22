package ru.itwebs.cms.catalog;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.List;
import java.util.Set;

public final class CatalogDtos {
    private CatalogDtos() { }

    public enum Kind { SERVICES, CASES, SPECIALISTS, TEAM }

    public record Item(@NotBlank @Size(max = 500) String title,
                       @Size(max = 20000) String description, @Size(max = 240) String price,
                       @Size(max = 240) String label) {
        public Item(String title, String description, String price) { this(title, description, price, null); }
    }
    public record Faq(@NotBlank @Size(max = 1000) String question,
                      @NotBlank @Size(max = 20000) String answer) { }
    public record Technology(@NotBlank @Size(max = 120) String name, @Positive Long mediaId) { }
    public record Picture(@Positive Long mediaId, @Size(max = 1000) String sourceUrl, @Size(max = 255) String alt,
                          @Size(max = 1000) String caption) { }
    public record ServiceContent(
            @Size(max = 500) String examplesTitle, @Size(max = 500) String faqTitle,
            @Size(max = 500) String stackTitle, @Size(max = 20000) String stackDescription,
            @Size(max = 500) String processTitle, @Size(max = 500) String tasksTitle,
            @Size(max = 500) String audienceTitle, @Size(max = 20000) String audienceDescription,
            @Size(max = 500) String cooperationTitle,
            @NotNull @Size(max = 100) List<@NotNull @Valid Faq> faq,
            @NotNull @Size(max = 100) List<@NotNull @Valid Technology> stack,
            @NotNull @Size(max = 100) List<@NotNull @Valid Item> process,
            @NotNull @Size(max = 100) List<@NotNull @Valid Item> tasks,
            @NotNull @Size(max = 100) List<@NotNull @Valid Item> audience,
            @NotNull @Size(max = 100) List<@NotNull @Valid Item> cooperation) { }
    public enum CaseSectionKind {
        @com.fasterxml.jackson.annotation.JsonProperty("intro") INTRO,
        @com.fasterxml.jackson.annotation.JsonProperty("service") SERVICE,
        @com.fasterxml.jackson.annotation.JsonProperty("stats") STATS,
        @com.fasterxml.jackson.annotation.JsonProperty("problem") PROBLEM,
        @com.fasterxml.jackson.annotation.JsonProperty("approach") APPROACH,
        @com.fasterxml.jackson.annotation.JsonProperty("solution") SOLUTION,
        @com.fasterxml.jackson.annotation.JsonProperty("stack") STACK,
        @com.fasterxml.jackson.annotation.JsonProperty("gallery") GALLERY,
        @com.fasterxml.jackson.annotation.JsonProperty("results") RESULTS
    }
    public record CaseSectionItem(@Size(max = 500) String title, @Size(max = 20000) String description,
                                  @Size(max = 120) String value, @Size(max = 120) String unit) { }
    public record CaseSection(@NotNull CaseSectionKind kind, @Size(max = 500) String title,
                              @Size(max = 50000) String description,
                              @NotNull @Size(max = 100) List<@NotNull @Valid CaseSectionItem> items) { }
    public record SeoLink(@NotBlank @Size(max = 500) String title, @NotBlank @Size(max = 1000) String href) { }
    public record CaseSeo(@Size(max = 500) String title, @Size(max = 50000) String description,
                          @NotNull @Size(max = 100) List<@NotNull @Valid SeoLink> links) { }
    public record CaseContent(@Size(max = 500) String headline, @Size(max = 500) String client, @Size(max = 50000) String challenge,
                              @Size(max = 50000) String solution, @Size(max = 50000) String result,
                              @NotNull @Size(max = 100) List<@NotNull @Valid Item> sections,
                              @NotNull @Size(max = 100) List<@NotNull @Valid Technology> stack,
                              @NotNull @Size(max = 100) List<@NotNull @Valid Picture> gallery,
                              @io.swagger.v3.oas.annotations.media.Schema(description = "Типизированные секции; старое sections сохранено отдельно. stack/gallery используют списки project.stack/project.gallery; service использует serviceId. Для полной новой формы передавайте sectionsV2 и tags, в том числе пустые списки.")
                              @Size(max = 100) List<@NotNull @Valid CaseSection> sectionsV2,
                              @Size(max = 30) List<@NotBlank @Size(max = 120) String> tags,
                              @io.swagger.v3.oas.annotations.media.Schema(description = "ID услуги каталога. Связь хранится внешним ключом. В полной новой форме null снимает связь; из sectionsV2 при этом нужно убрать секцию service.")
                              @Positive Long serviceId,
                              @com.fasterxml.jackson.annotation.JsonProperty(access = com.fasterxml.jackson.annotation.JsonProperty.Access.READ_ONLY) String serviceSlug,
                              @Valid CaseSeo seo) {
        public CaseContent(String headline, String client, String challenge, String solution, String result,
                           List<Item> sections, List<Technology> stack, List<Picture> gallery) {
            this(headline, client, challenge, solution, result, sections, stack, gallery, null, null, null, null, null);
        }
    }
    public record SpecialistContent(@Size(max = 120) String direction,
                                    @NotNull @Size(max = 20) List<@NotBlank @Size(max = 120) String> grades,
                                    @Size(max = 120) String experience, @Size(max = 240) String availability,
                                    @NotNull @Size(max = 100) List<@NotBlank @Size(max = 240) String> skills) { }
    public record MemberContent(@Size(max = 240) String position, @Size(max = 20000) String biography) { }
    public record EntryRequest(
            @NotBlank @Size(max = 240) String title,
            @NotBlank @Size(max = 160) @Pattern(regexp = "[a-z0-9]+(?:-[a-z0-9]+)*") String slug,
            @Size(max = 50000) String description, @Size(max = 240) String price,
            @Size(max = 240) String seoTitle, @Size(max = 1000) String seoDescription,
            boolean published, @Min(0) int sortOrder, @Positive Long coverMediaId, @Positive Long mobileCoverMediaId,
            @NotNull @Size(max = 20) Set<@NotNull @Positive Long> categoryIds,
            @NotNull @Size(max = 100) List<@NotNull @Positive Long> caseIds,
            @Valid ServiceContent service, @Valid CaseContent project,
            @Valid SpecialistContent specialist, @Valid MemberContent member,
            @PositiveOrZero Long version, @Valid ServiceComponentDtos.Selections serviceSelections,
            @io.swagger.v3.oas.annotations.media.Schema(description = "Основной адрес реальной страницы сайта, включая query при необходимости. null сохраняет прежний адрес; пустая строка очищает. Для услуги без настройки публичный canonical=null.")
            @Size(max = 1000) String canonical) { }
    public record CategoryRequest(@NotBlank @Size(max = 120) String name,
                                  @NotBlank @Size(max = 80) @Pattern(regexp = "[a-z0-9]+(?:-[a-z0-9]+)*") String slug,
                                  @Min(0) int sortOrder, @PositiveOrZero Long version) { }
    public record CategoryView(Long id, Long version, Kind kind, String name, String slug, int sortOrder) { }
    public record MediaView(Long id, String url, String alt) { }
    public record Card(Long id, Long version, Kind kind, String title, String slug, String link,
                       String description, String price, boolean published, int sortOrder,
                       MediaView cover, MediaView mobileCover, List<CategoryView> categories,
                       SpecialistContent specialist, MemberContent member) { }
    public record EntryView(Card entry, String seoTitle, String seoDescription,
                            ServiceContent service, CaseContent project, SpecialistContent specialist,
                            MemberContent member, List<Card> examples, List<MediaView> images,
                            ServiceComponentDtos.Selections serviceSelections, String canonical, Card linkedService) { }
    public record EntryPage(List<Card> items, int page, int size, long totalElements, int totalPages) { }
}
