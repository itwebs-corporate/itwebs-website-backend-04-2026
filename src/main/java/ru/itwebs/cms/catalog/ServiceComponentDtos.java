package ru.itwebs.cms.catalog;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.List;

public final class ServiceComponentDtos {
    private ServiceComponentDtos() { }
    public enum ComponentKind { FAQ, STACK, PROCESS, TASK, AUDIENCE, COOPERATION }
    public record ComponentRequest(@NotBlank @Size(max = 1000) String title,
                                   @Size(max = 20000) String description, @Size(max = 240) String price,
                                   @Positive Long mediaId, @Min(0) int sortOrder, @PositiveOrZero Long version) { }
    public record ComponentView(Long id, Long version, ComponentKind kind, String title, String description,
                                String price, CatalogDtos.MediaView image, int sortOrder) { }
    public record ComponentPage(List<ComponentView> items, int page, int size, long totalElements, int totalPages) { }
    public record ComponentOverride(@Size(max = 1000) String title, @Size(max = 20000) String description,
                                    @Size(max = 240) String price) { }
    public record Selection(@NotNull @Positive Long componentId, @Valid ComponentOverride override) { }
    public record Selections(
            @NotNull @Size(max = 100) List<@NotNull @Valid Selection> faq,
            @NotNull @Size(max = 100) List<@NotNull @Valid Selection> stack,
            @NotNull @Size(max = 100) List<@NotNull @Valid Selection> process,
            @NotNull @Size(max = 100) List<@NotNull @Valid Selection> tasks,
            @NotNull @Size(max = 100) List<@NotNull @Valid Selection> audience,
            @NotNull @Size(max = 100) List<@NotNull @Valid Selection> cooperation) { }
}
