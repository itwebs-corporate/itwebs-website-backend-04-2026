package ru.itwebs.cms.bootstrap;

import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.itwebs.cms.entity.ContentItem;
import ru.itwebs.cms.entity.SiteBlock;
import ru.itwebs.cms.entity.SitePage;
import ru.itwebs.cms.pages.PageDtos.*;
import ru.itwebs.cms.repository.ContentRepository;
import ru.itwebs.cms.repository.SiteBlockRepository;
import ru.itwebs.cms.repository.SitePageRepository;
import java.util.*;

/** Adds editable schema slots only; no website copy, links or images are populated. */
@Component
@Order(130)
public class PageFieldsMigration implements CommandLineRunner {
    private static final String MARKER = "pageFieldsV4";
    private final ContentRepository markers;
    private final SitePageRepository pages;
    private final SiteBlockRepository blocks;
    private final JdbcTemplate jdbc;

    public PageFieldsMigration(ContentRepository markers, SitePageRepository pages, SiteBlockRepository blocks, JdbcTemplate jdbc) {
        this.markers = markers;
        this.pages = pages;
        this.blocks = blocks;
        this.jdbc = jdbc;
    }

    @Override
    @Transactional
    public void run(String... args) {
        jdbc.execute("select pg_advisory_xact_lock(738245981)");
        if (markers.existsBySectionNameAndItemKeyAndGroupName("_system", MARKER, null)) return;

        link("shared", "header", "Шапка сайта", BlockType.NAVIGATION, "cta", "Кнопка в шапке");
        text("shared", "contact", "Форма заявки", BlockType.FORM, "error", "Текст ошибки отправки заявки");

        Map<String, String> heroes = new LinkedHashMap<>();
        heroes.put("home", "hero");
        heroes.put("services", "services-page");
        heroes.put("cases", "cases-page");
        heroes.put("outstaff", "outstaff-page");
        heroes.put("product", "product-page");
        heroes.put("blog", "blog-page");
        heroes.put("blog-article", "blog-article-page");
        heroes.put("service-detail", "service-detail-page");
        heroes.put("case-detail", "case-detail-page");
        heroes.forEach((page, key) -> {
            link(page, key, "Первый экран", BlockType.HERO, "floatingCta", "Основная плавающая кнопка");
            link(page, key, "Первый экран", BlockType.HERO, "floatingSecondary", "Вторая плавающая кнопка");
        });

        SitePage home = page("home");
        SiteBlock words = block(home, "hero-title-words", "Сменяемые слова первого экрана", BlockType.CARDS);
        SiteBlock hero = home.blocks.stream().filter(item -> item.key.equals("hero")).findFirst().orElseThrow();
        // Equal order puts the new block after the older hero by ID, before the next section.
        if (words.id == null) words.sortOrder = hero.sortOrder;
        itemField(words, "word", "Слово первого экрана", "TEXT");
        itemField(block(home, "stat-cards", "Показатели компании", BlockType.CARDS),
                "suffix", "Знак после числа", "TEXT");
        field(block(page("product"), "product-page-use-case-3", "Третий пример продукта", BlockType.COPY),
                "imageMobile", "Мобильное изображение примера", "IMAGE");

        link("home", "brands", "Клиенты и партнёры", BlockType.CARDS, "cta", "Кнопка под брендами");
        text("home", "services", "Услуги", BlockType.CARDS, "casesLabel", "Подпись кнопки проектов услуги");
        text("home", "projects", "Проекты", BlockType.CARDS, "moreLabel", "Подпись кнопки подробностей кейса");
        text("home", "staffing", "Специалисты", BlockType.CARDS, "orderLabel", "Подпись кнопки заказа специалиста");

        text("services", "services-page", "Первый экран услуг", BlockType.HERO, "detailLabel", "Подпись кнопки подробностей услуги");
        text("services", "services-page", "Первый экран услуг", BlockType.HERO, "casesLabel", "Подпись кнопки проектов услуги");
        text("cases", "cases-page", "Первый экран проектов", BlockType.HERO, "allLabel", "Подпись вкладки всех проектов");
        text("cases", "cases-page", "Первый экран проектов", BlockType.HERO, "moreLabel", "Подпись кнопки подробностей кейса");

        text("outstaff", "outstaff-page-team", "Команда аутстаффинга", BlockType.CARDS, "title", "Заголовок команды");
        text("outstaff", "outstaff-page-team", "Команда аутстаффинга", BlockType.CARDS, "eyebrow", "Надзаголовок команды");
        text("outstaff", "outstaff-page-specialists", "Специалисты аутстаффинга", BlockType.CARDS, "allLabel", "Подпись вкладки всех специалистов");
        text("outstaff", "outstaff-page-specialists", "Специалисты аутстаффинга", BlockType.CARDS, "emptyLabel", "Текст пустого списка специалистов");
        text("outstaff", "outstaff-page", "Первый экран аутстаффинга", BlockType.HERO, "badgeMobile", "Мобильный бейдж первого экрана");

        for (var label : Map.of("name", "наименования компании", "address", "юридического адреса", "innKpp", "ИНН/КПП", "inn", "ИНН", "kpp", "КПП", "ogrn", "ОГРН").entrySet()) {
            text("contacts", "contacts-company", "Реквизиты", BlockType.COPY, label.getKey() + "Label", "Подпись " + label.getValue());
            text("contacts", "contacts-company", "Реквизиты", BlockType.COPY, label.getKey() + "LabelMobile", "Короткая мобильная подпись " + label.getValue());
        }

        SitePage policy = page("policy");
        SiteBlock policyHero = block(policy, "policy-page", "Первый экран политики", BlockType.HERO);
        if (policyHero.id == null) {
            int first = policy.blocks.stream().filter(item -> item != policyHero).mapToInt(item -> item.sortOrder).min().orElse(0);
            policyHero.sortOrder = first == Integer.MIN_VALUE ? first : first - 1;
        }
        field(policyHero, "title", "Заголовок первого экрана", "TEXT");
        field(policyHero, "badgeMobile", "Мобильный бейдж первого экрана", "TEXT");

        // Persist new blocks through the existing page-to-block cascade.
        pages.saveAll(pages.findAllByOrderByIdAsc());
        pages.flush();
        blocks.flush();
        ContentItem marker = new ContentItem();
        marker.setSectionName("_system");
        marker.setItemKey(MARKER);
        marker.setLabel("Page field schemas V4");
        marker.setValue("complete");
        marker.setPublished(false);
        markers.save(marker);
    }

    private SitePage page(String slug) {
        return pages.findBySlug(slug).orElseThrow(() -> new IllegalStateException("Не найдена страница для расширения схемы: " + slug));
    }

    private SiteBlock block(SitePage page, String key, String title, BlockType type) {
        return page.blocks.stream().filter(item -> item.key.equals(key)).findFirst().orElseGet(() -> {
            SiteBlock block = new SiteBlock();
            block.page = page;
            block.key = key;
            block.title = title;
            block.type = type;
            block.published = false;
            block.sortOrder = page.blocks.stream().mapToInt(item -> item.sortOrder).max().orElse(-1) + 1;
            block.schema = new BlockSchema(Map.of(), Map.of());
            block.data = new BlockData(Map.of(), List.of());
            page.blocks.add(block);
            return block;
        });
    }

    private void text(String page, String block, String title, BlockType type, String key, String label) {
        field(block(page(page), block, title, type), key, label, "TEXT");
    }

    private void link(String page, String block, String title, BlockType type, String key, String label) {
        field(block(page(page), block, title, type), key, label, "LINK");
    }

    private void field(SiteBlock block, String key, String label, String type) {
        Map<String, FieldRule> schema = new LinkedHashMap<>(block.schema.fields());
        Map<String, Value> fields = new LinkedHashMap<>(block.data.fields());
        addRule(schema, key, label, type);
        fields.putIfAbsent(key, new Value(null, null, null));
        block.schema = new BlockSchema(schema, block.schema.itemFields());
        block.data = new BlockData(fields, block.data.items());
    }

    private void itemField(SiteBlock block, String key, String label, String type) {
        Map<String, FieldRule> schema = new LinkedHashMap<>(block.schema.itemFields());
        addRule(schema, key, label, type);
        List<Map<String, Value>> rows = new ArrayList<>();
        for (var row : block.data.items()) {
            Map<String, Value> copy = new LinkedHashMap<>(row);
            copy.putIfAbsent(key, new Value(null, null, null));
            rows.add(copy);
        }
        block.schema = new BlockSchema(block.schema.fields(), schema);
        block.data = new BlockData(block.data.fields(), rows);
    }

    private void addRule(Map<String, FieldRule> schema, String key, String label, String type) {
        FieldRule existing = schema.putIfAbsent(key, new FieldRule(label, type));
        if (existing != null && !existing.type().equals(type))
            throw new IllegalStateException("Тип существующего поля не совпадает с расширением схемы: " + key);
    }
}
