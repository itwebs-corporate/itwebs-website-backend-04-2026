package ru.itwebs.cms.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import ru.itwebs.cms.service.PageService;
import ru.itwebs.cms.pages.PageDtos.*;
import java.util.List;

@RestController
@RequestMapping("/api/admin/pages")
@Tag(name = "Админ: страницы", description = "Каркас и схемы блоков фиксированы. Можно менять контент, публикацию и связанные записи. Общие блоки редактируются на странице shared. floating-cta задаёт общие кнопки floatingCta и floatingSecondary типа LINK (подпись и адрес). После заполнения и публикации блок включается в ответы всех опубликованных страниц. Существующие одноимённые поля отдельных страниц сохранены для индивидуальных настроек. Новые поля добавляются без наполнения; новые блоки создаются черновиками. hero-title-words содержит строки с полем word; policy-page — title и badgeMobile. Кнопки имеют тип LINK: text и href.")
@SecurityRequirement(name = "basicAuth")
public class AdminPageController {
    private final PageService pages;
    public AdminPageController(PageService pages) { this.pages = pages; }
    @GetMapping @Operation(summary = "Все страницы")
    public List<PageCard> list() { return pages.list(); }
    @GetMapping("/{slug}") @Operation(summary = "Страница, схемы форм и привязки каталогов")
    public AdminPage get(@PathVariable String slug) { return pages.admin(slug); }
    @PutMapping("/{slug}") @Operation(summary = "Название, SEO и публикация страницы")
    public AdminPage save(@PathVariable String slug, @Valid @RequestBody PageRequest request) { return pages.save(slug, request); }
    @PutMapping("/{slug}/blocks/{id}") @Operation(summary = "Сохранить фиксированный блок", description = "data.fields — поля, data.items — строки списка. Ключи и типы соответствуют schema из GET, включая новые пустые поля. version обязательна. Для публикации заполненного нового блока передайте published=true.")
    public BlockView block(@PathVariable String slug, @PathVariable Long id, @Valid @RequestBody BlockRequest request) { return pages.saveBlock(slug, id, request); }
    @PutMapping("/{slug}/collections/{id}") @Operation(summary = "Выбрать записи для страницы", description = "allEntries=true и entryIds=[] — весь каталог; иначе выбранные ID в порядке отображения.")
    public CollectionView collection(@PathVariable String slug, @PathVariable Long id, @Valid @RequestBody CollectionRequest request) { return pages.saveCollection(slug, id, request); }
}
