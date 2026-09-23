package ru.itwebs.cms.dto;

import jakarta.validation.constraints.NotNull;

public record LeadStatusRequest(@NotNull Boolean processed) { }
