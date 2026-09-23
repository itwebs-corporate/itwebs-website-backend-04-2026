package ru.itwebs.cms.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ContentRequest(
        @NotBlank @Size(max = 80) String sectionName,
        @NotBlank @Size(max = 80) String itemKey,
        @Size(max = 80) String groupName,
        @NotBlank @Pattern(regexp = "TEXT|LINK|IMAGE") String type,
        @Size(max = 120) String label,
        String value,
        @Size(max = 1000) String href,
        Long mediaId,
        int sortOrder,
        @NotNull Boolean published) { }
