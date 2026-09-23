package ru.itwebs.cms.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record MediaUpdateRequest(@NotBlank @Size(max = 255) String altText) { }
