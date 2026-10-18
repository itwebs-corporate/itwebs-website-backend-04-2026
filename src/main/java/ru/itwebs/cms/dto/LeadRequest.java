package ru.itwebs.cms.dto;

import jakarta.validation.constraints.*;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

public record LeadRequest(
        @NotBlank @Size(max = 120) String name,
        @Size(max = 80) String phone,
        @Email @Size(max = 255) String email,
        @Schema(description = "Необязательное описание задачи. Можно пропустить, передать null или пустую строку.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        @Size(max = 5000) String message,
        @Pattern(regexp = "phone|email|telegram|whatsapp") String contactMethod,
        @AssertTrue boolean consent,
        @Size(max = 120) String specialist,
        @Size(max = 20) List<@NotBlank @Size(max = 120) String> grades,
        @Size(max = 1000) String sourcePage) {
    public LeadRequest(String name, String phone, String email, String message, String contactMethod, boolean consent) {
        this(name, phone, email, message, contactMethod, consent, null, null, null);
    }
}
