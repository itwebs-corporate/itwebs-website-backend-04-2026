package ru.itwebs.cms.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.Collection;
import java.util.List;
import ru.itwebs.cms.entity.BlogArticle;
import java.time.Instant;
import java.util.Optional;

public interface BlogArticleRepository extends JpaRepository<BlogArticle, Long> {
    String CARD_FIELDS = "select a.id as id, a.version as version, a.title as title, a.slug as slug, "
            + "a.excerpt as excerpt, m.id as coverMediaId, m.altText as coverAlt, "
            + "a.readingMinutes as readingMinutes, a.status as status, a.publishedAt as publishedAt, "
            + "a.createdAt as createdAt, a.updatedAt as updatedAt from BlogArticle a left join a.cover m ";
    String CARD_FILTER = "where (:publicOnly = false or (a.status = 'PUBLISHED' and a.publishedAt <= :now)) "
            + "and (:status is null or a.status = :status) "
            + "and (:excludedId is null or a.id <> :excludedId) "
            + "and (:categorySlug is null or exists (select c.id from BlogArticle categorized "
            + "join categorized.categories c where categorized.id = a.id and c.slug = :categorySlug))";

    @Query(value = CARD_FIELDS + CARD_FILTER, countQuery = "select count(a) from BlogArticle a " + CARD_FILTER)
    Page<BlogCardRow> findCards(@Param("publicOnly") boolean publicOnly, @Param("now") Instant now,
                               @Param("status") String status, @Param("categorySlug") String categorySlug,
                               @Param("excludedId") Long excludedId, Pageable pageable);

    @Query("select a.id as articleId, c as category from BlogArticle a join a.categories c where a.id in :ids")
    List<BlogCategoryLink> findCategoryLinks(@Param("ids") Collection<Long> ids);

    @Query("select a.id from BlogArticle a where a.slug = :slug and a.status = 'PUBLISHED' and a.publishedAt <= :now")
    Optional<Long> findPublishedId(@Param("slug") String slug, @Param("now") Instant now);

    boolean existsBySlugAndIdNot(String slug, Long id);
    boolean existsByCategoriesId(Long id);
    boolean existsByImagesId(Long id);
    Optional<BlogArticle> findBySlugAndStatusAndPublishedAtLessThanEqual(String slug, String status, Instant now);
}
