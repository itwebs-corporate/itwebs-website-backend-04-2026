package ru.itwebs.cms.dto;

import ru.itwebs.cms.catalog.CatalogDtos.EntryView;

public record DetailPageResponse(PageResponse layout, EntryView content) { }
