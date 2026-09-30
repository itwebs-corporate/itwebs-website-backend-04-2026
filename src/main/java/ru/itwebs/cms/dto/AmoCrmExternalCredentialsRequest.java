package ru.itwebs.cms.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AmoCrmExternalCredentialsRequest(
        @NotBlank @Size(max = 500) String client_id,
        @NotBlank @Size(max = 2000) String client_secret,
        @NotBlank @Size(max = 100) String state) { }
