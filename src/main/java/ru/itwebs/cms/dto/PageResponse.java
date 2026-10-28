package ru.itwebs.cms.dto;

import ru.itwebs.cms.pages.PageDtos.BlockView;
import ru.itwebs.cms.catalog.CatalogDtos.Card;
import ru.itwebs.cms.catalog.CatalogDtos.MediaView;
import java.util.List;
import java.util.Map;

public record PageResponse(String page, String path, String title, String seoTitle, String seoDescription,
                            String canonical, List<BlockView> blocks, Map<String, List<Card>> collections,
                            List<MediaView> images, List<String> technologies, List<String> categorySlugs) { }
