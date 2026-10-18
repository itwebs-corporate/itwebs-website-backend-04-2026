package ru.itwebs.cms.dto;

import ru.itwebs.cms.blog.BlogDtos.ArticleResponse;
import ru.itwebs.cms.blog.BlogDtos.Card;
import java.util.List;

public record ArticlePageResponse(PageResponse layout, ArticleResponse content, List<Card> related) { }
