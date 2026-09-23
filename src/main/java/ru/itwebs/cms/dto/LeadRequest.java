package ru.itwebs.cms.dto;

import jakarta.validation.constraints.*;

public record LeadRequest(
        @NotBlank @Size(max = 120) String name,
        @Size(max = 80) String phone,
        @Email @Size(max = 255) String email,
        @NotBlank @Size(max = 5000) String message,
        @Pattern(regexp = "phone|email|telegram|whatsapp") String contactMethod,
        @AssertTrue boolean consent) { }
