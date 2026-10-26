package ru.itwebs.cms.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;
import ru.itwebs.cms.dto.PageResponse;
import ru.itwebs.cms.service.PageService;
import ru.itwebs.cms.catalog.CatalogService;
import ru.itwebs.cms.catalog.CatalogDtos.Kind;
import ru.itwebs.cms.catalog.CatalogDtos.EntryView;
import ru.itwebs.cms.blog.BlogDtos.ArticleResponse;
import ru.itwebs.cms.blog.BlogService;
import ru.itwebs.cms.dto.DetailPageResponse;
import ru.itwebs.cms.dto.ArticlePageResponse;

@RestController
@RequestMapping("/api/pages")
@Tag(name = "Страницы", description = "Фиксированные страницы: home, services, cases, outstaff, product, blog, blog-article, contacts, policy. Общие блоки включены. Детальные услуги/кейсы/специалисты — /api/catalog/{kind}/{slug}, статьи — /api/blog/articles/{slug}.")
public class PublicPageController {
    private final PageService pages;
    private final CatalogService catalog;
    private final BlogService blog;
    public PublicPageController(PageService pages, CatalogService catalog, BlogService blog) {
        this.pages = pages; this.catalog = catalog; this.blog = blog;
    }
    @GetMapping({"", "/{slug}"})
    @Operation(summary = "Контент страницы", description = "blocks — фиксированный каркас, collections — связанные карточки (до 100; полный каталог с пагинацией /api/catalog/{kind}). images — изображения блоков. shared — общий блок. Опубликованный блок floating-cta с ownerPage=shared содержит ссылки по умолчанию в data.fields.floatingCta и floatingSecondary; он включается также в шаблоны детальных страниц. Непустые одноимённые поля собственной страницы могут использоваться фронтендом как индивидуальные настройки; API не объединяет их с общим блоком автоматически. Строки выводите как текст.")
    public PageResponse get(@PathVariable(required = false) String slug) { return pages.published(slug == null ? "home" : slug); }

    @GetMapping("/{section:services|cases|outstaff|team}/{slug}")
    @Operation(summary = "Детальная страница услуги, кейса или специалиста", description = "layout содержит общие блоки; content — данные записи с собственными блоками. В content.project.sectionsV2 находятся типизированные секции кейса; stack/gallery берутся из одноимённых списков project. content.linkedService содержит карточку связанной опубликованной услуги. Для услуги без настроенного основного адреса layout.canonical=null. Неопубликованная запись или скрытый шаблон даёт 404.")
    public DetailPageResponse detail(@PathVariable String section, @PathVariable String slug) {
        Kind kind = switch (section) {
            case "services" -> Kind.SERVICES;
            case "cases" -> Kind.CASES;
            case "outstaff" -> Kind.SPECIALISTS;
            default -> Kind.TEAM;
        };
        String template = switch (kind) {
            case SERVICES -> "service-detail";
            case CASES -> "case-detail";
            case SPECIALISTS -> "specialist-detail";
            case TEAM -> "team-detail";
        };
        EntryView content = catalog.published(kind, slug);
        return new DetailPageResponse(metadata(pages.published(template), content.entry().link(), content.entry().title(),
                content.seoTitle(), content.seoDescription(), content.canonical()), content);
    }
    @GetMapping("/blog/{slug}")
    @Operation(summary = "Детальная страница статьи с общими блоками и рекомендациями")
    public ArticlePageResponse article(@PathVariable String slug) {
        ArticleResponse content = blog.published(slug);
        return new ArticlePageResponse(metadata(pages.published("blog-article"), content.article().url(), content.article().title(),
                content.seoTitle(), content.seoDescription(), content.article().url()), content, blog.related(slug, 3));
    }
    private PageResponse metadata(PageResponse layout, String path, String title, String seoTitle, String seoDescription, String canonical) {
        return new PageResponse(layout.page(), path, title, seoTitle, seoDescription, canonical,
                layout.blocks(), layout.collections(), layout.images());
    }
}
