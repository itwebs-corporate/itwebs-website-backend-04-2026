package ru.itwebs.cms.repository;

import ru.itwebs.cms.entity.BlogCategory;

public interface BlogCategoryLink {
    Long getArticleId();
    BlogCategory getCategory();
}
