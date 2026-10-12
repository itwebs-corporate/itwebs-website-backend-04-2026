package ru.itwebs.cms.dto;

import jakarta.validation.constraints.*;
import io.swagger.v3.oas.annotations.media.Schema;

public record LeadRequest(
        @NotBlank @Size(max = 120) String name,
        @Size(max = 80) String phone,
        @Email @Size(max = 255) String email,
        @Schema(description = "Необязательное описание задачи. Можно пропустить, передать null или пустую строку.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        @Size(max = 5000) String message,
        @Pattern(regexp = "phone|email|telegram|whatsapp") String contactMethod,
        @AssertTrue boolean consent) { }
