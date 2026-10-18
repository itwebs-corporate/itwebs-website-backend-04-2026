package ru.itwebs.cms.bootstrap;

import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.itwebs.cms.catalog.CatalogDtos.*;
import ru.itwebs.cms.entity.*;
import ru.itwebs.cms.pages.PageDtos.*;
import ru.itwebs.cms.repository.*;
import java.util.*;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.ClassPathResource;

@Component
@Order(100)
public class StructuredContentMigration implements CommandLineRunner {
    private static final String MARKER = "structuredCmsV1";
    private static final Set<String> SHARED = Set.of("header", "contact", "footer", "footer-navigation", "footer-services", "footer-regions");
    private static final Set<String> CATALOG_SECTIONS = Set.of("services-page-catalog", "service-cards", "service-categories",
            "project-cards", "cases-page-items", "cases-page-filters", "outstaff-page-specialist-items", "outstaff-page-filters",
            "staffing-cards", "team-members", "outstaff-page-team-overrides", "services-page-project-items",
            "product-page-use-case-items", "blog-article-service-items");
    private static final Map<String, List<String>> LAYOUTS = Map.of(
            "shared", List.of("header", "contact", "footer", "footer-navigation", "footer-services", "footer-regions"),
            "home", List.of("hero", "brands", "services", "projects", "stats", "stat-cards", "technologies", "technology-tabs", "technology-items", "staffing", "team", "seo", "seo-services"),
            "services", List.of("services-page", "services-page-title-words", "services-page-stacks", "services-page-stack-items", "services-page-process", "services-page-process-steps", "services-page-tasks", "services-page-audience", "services-page-audiences", "services-page-cooperation", "services-page-cooperation-models", "services-page-projects", "services-page-seo", "services-page-seo-topics"),
            "cases", List.of("cases-page", "cases-page-seo", "cases-page-seo-links"),
            "outstaff", List.of("outstaff-page", "outstaff-page-use-cases", "outstaff-page-use-case-items", "outstaff-page-specialists", "outstaff-page-team", "outstaff-page-process", "outstaff-page-process-steps", "outstaff-page-terms", "outstaff-page-term-items", "outstaff-page-contact", "outstaff-page-contact-specialists", "outstaff-page-contact-grades", "outstaff-page-seo", "outstaff-page-seo-items"),
            "product", List.of("product-page", "product-page-advantages", "product-page-advantage-items", "product-page-demo", "product-page-control", "product-page-control-items", "product-page-use-cases", "product-page-use-case-1", "product-page-use-case-2", "product-page-use-case-3", "product-page-stages", "product-page-stage-items", "product-page-audience", "product-page-audience-items", "product-page-contact", "product-page-seo"),
            "blog", List.of("blog-page", "blog-page-title-words", "blog-page-seo", "blog-page-seo-items"),
            "blog-article", List.of("blog-article-page", "blog-article-page-seo", "blog-article-page-seo-topics"));
    private final ContentRepository legacy;
    private final CatalogRepository catalog;
    private final CatalogCategoryRepository categories;
    private final SitePageRepository pages;
    private final JdbcTemplate jdbc;
    private Map<String, List<ContentItem>> sections;
    private final Map<String, CaseStudy> projects = new LinkedHashMap<>();
    private final Map<String, ServiceOffering> services = new LinkedHashMap<>();
    private final Map<String, TeamMember> team = new LinkedHashMap<>();
    private final Map<String, OutstaffSpecialist> specialists = new LinkedHashMap<>();
    private final Map<String, CatalogCategory> categoryIndex = new HashMap<>();

    public StructuredContentMigration(ContentRepository legacy, CatalogRepository catalog,
                                      CatalogCategoryRepository categories, SitePageRepository pages, JdbcTemplate jdbc) {
        this.legacy = legacy;
        this.catalog = catalog;
        this.categories = categories;
        this.pages = pages;
        this.jdbc = jdbc;
    }

    @Override
    @Transactional
    public void run(String... args) {
        jdbc.execute("select pg_advisory_xact_lock(738245981)");
        if (legacy.existsBySectionNameAndItemKeyAndGroupName("_system", MARKER, null)) return;
        sections = new LinkedHashMap<>();
        legacy.findAllByOrderBySectionNameAscSortOrderAscIdAsc().forEach(item ->
                sections.computeIfAbsent(item.getSectionName(), ignored -> new ArrayList<>()).add(item));
        migrateCases();
        migrateCaseDetails();
        migrateServices();
        migratePeople();
        Map<String, SitePage> layouts = new LinkedHashMap<>();
        String[][] definitions = {{"shared", "/_shared", "Общие блоки"}, {"home", "/", "Главная"},
                {"services", "/services", "Услуги"}, {"cases", "/cases", "Проекты"},
                {"outstaff", "/outstaff", "Аутстаффинг"}, {"product", "/product", "Продукт"},
                {"blog", "/blog", "Блог"}, {"blog-article", "/blog/:slug", "Шаблон статьи"},
                {"service-detail", "/services/:slug", "Шаблон услуги"}, {"case-detail", "/cases/:slug", "Шаблон кейса"},
                {"specialist-detail", "/outstaff/:slug", "Шаблон специалиста"}, {"team-detail", "/team/:slug", "Шаблон участника команды"},
                {"contacts", "/contacts", "Контакты"}, {"policy", "/policy", "Политика в отношении обработки персональных данных"}};
        for (String[] definition : definitions) {
            SitePage page = new SitePage();
            page.slug = definition[0];
            page.path = definition[1];
            page.title = definition[2];
            page.seoTitle = page.title + " — ITWEBS";
            layouts.put(page.slug, page);
        }
        sections.forEach((section, items) -> {
            if (section.startsWith("_system") || CATALOG_SECTIONS.contains(section)) return;
            String owner = owner(section);
            SitePage page = layouts.get(owner);
            if (section.endsWith("-meta") || section.equals("meta")) {
                Map<String, ContentItem> fields = fields(items);
                page.seoTitle = text(fields, "title", page.seoTitle);
                page.seoDescription = text(fields, "description", null);
                page.canonical = text(fields, "canonical", null);
            } else page.blocks.add(block(page, section, items));
        });
        addStaticPages(layouts);
        migrateProductUseCases(layouts.get("product"));
        layouts.forEach((slug, page) -> {
            List<String> order = LAYOUTS.getOrDefault(slug, List.of());
            page.blocks.sort(Comparator.comparingInt(block -> order.contains(block.key) ? order.indexOf(block.key) : order.size()));
            for (int index = 0; index < page.blocks.size(); index++) page.blocks.get(index).sortOrder = index;
        });
        reuseSeo(layouts.get("product"), layouts.get("services"));
        reuseSeo(layouts.get("service-detail"), layouts.get("services"));
        reuseSeo(layouts.get("specialist-detail"), layouts.get("outstaff"));
        reuseSeo(layouts.get("team-detail"), layouts.get("home"));
        collection(layouts.get("home"), "services", Kind.SERVICES, true, List.of());
        collection(layouts.get("home"), "cases", Kind.CASES, false, new ArrayList<>(projects.values()).stream().limit(6).toList());
        migrateHomeSpecialists(layouts.get("home"));
        collection(layouts.get("home"), "team", Kind.TEAM, true, List.of());
        collection(layouts.get("services"), "services", Kind.SERVICES, true, List.of());
        collection(layouts.get("services"), "cases", Kind.CASES, false, linkedProjects("services-page-project-items"));
        collection(layouts.get("cases"), "cases", Kind.CASES, false, linkedProjects("cases-page-items"));
        collection(layouts.get("outstaff"), "specialists", Kind.SPECIALISTS, false, new ArrayList<>(specialists.values()));
        PageCollection outstaffTeam = collection(layouts.get("outstaff"), "team", Kind.TEAM, false, new ArrayList<>(team.values()));
        groups("outstaff-page-team-overrides").forEach((group, fields) -> {
            TeamMember member = team.get(group);
            if (member != null) outstaffTeam.overrides.add(new DisplayOverride(member.id, text(fields, "name", null), null,
                    text(fields, "position", null), text(fields, "photoAlt", null)));
        });
        PageCollection productCases = collection(layouts.get("product"), "cases", Kind.CASES, false, linkedProjects("product-page-use-case-items"));
        groups("product-page-use-case-items").values().forEach(fields -> {
            CaseStudy project = projects.get(text(fields, "projectGroup", null));
            if (project != null) productCases.overrides.add(new DisplayOverride(project.id, text(fields, "projectTitle", null),
                    text(fields, "projectDescription", null), null, null));
        });
        collection(layouts.get("blog-article"), "services", Kind.SERVICES, false,
                groups("blog-article-service-items").values().stream().map(fields -> services.get(text(fields, "serviceGroup", null)))
                        .filter(Objects::nonNull).distinct().toList());
        pages.saveAll(layouts.values());
        sections.values().forEach(items -> items.forEach(item -> {
            if (item.getMedia() != null) {
                item.setArchivedMediaId(item.getMedia().getId());
                item.setMedia(null);
            }
        }));
        ContentItem marker = new ContentItem();
        marker.setSectionName("_system");
        marker.setItemKey(MARKER);
        marker.setLabel("Structured CMS migration");
        marker.setValue("complete");
        marker.setPublished(false);
        legacy.save(marker);
    }

    private String owner(String section) {
        if (SHARED.contains(section)) return "shared";
        if (section.startsWith("blog-article-")) return "blog-article";
        for (String slug : List.of("services", "cases", "outstaff", "product", "blog", "contacts", "policy")) {
            if (section.equals(slug + "-page") || section.startsWith(slug + "-page-")) return slug;
        }
        return "home";
    }
    private void reuseSeo(SitePage target, SitePage source) {
        source.blocks.stream().filter(block -> block.type == BlockType.SEO).forEach(target.reusedBlocks::add);
    }

    private void migrateProductUseCases(SitePage page) {
        groups("product-page-use-case-items").forEach((group, fields) -> {
            List<ContentItem> copy = fields.values().stream()
                    .filter(item -> !Set.of("projectGroup", "projectTitle", "projectDescription").contains(item.getItemKey())).toList();
            if (copy.isEmpty()) return;
            SiteBlock block = block(page, "product-page-" + group, copy);
            block.title = text(fields, "title", "Сценарий применения продукта");
            block.schema = new BlockSchema(block.schema.itemFields(), Map.of());
            block.data = new BlockData(block.data.items().getFirst(), List.of());
            page.blocks.add(block);
            CaseStudy project = projects.get(text(fields, "projectGroup", null));
            if (project != null) {
                PageCollection binding = collection(page, block.key, Kind.CASES, false, List.of(project));
                binding.overrides.add(new DisplayOverride(project.id, text(fields, "projectTitle", null),
                        text(fields, "projectDescription", null), null, null));
            }
        });
    }

    private void addStaticPages(Map<String, SitePage> layouts) {
        try (var input = new ClassPathResource("cms/static-pages.json").getInputStream()) {
            Map<String, String> copy = new ObjectMapper().readValue(input, new TypeReference<Map<String, String>>() { });
            copy.forEach((slug, text) -> {
                SitePage page = layouts.get(slug);
                if (!page.blocks.isEmpty()) return;
                if (slug.equals("contacts")) {
                    addContacts(page);
                    return;
                }
                if (slug.equals("policy")) {
                    addPolicy(page, text);
                    return;
                }
                SiteBlock block = new SiteBlock();
                block.page = page;
                block.key = slug + "-content";
                block.title = page.title;
                block.type = BlockType.COPY;
                block.schema = new BlockSchema(Map.of("body", new FieldRule("Текст страницы", "TEXT")), Map.of());
                block.data = new BlockData(Map.of("body", new Value(text, null, null)), List.of());
                page.blocks.add(block);
            });
        } catch (java.io.IOException error) {
            throw new IllegalStateException("Не удалось загрузить контент статических страниц", error);
        }
    }

    private record InitialBlock(String key, String title, BlockType type, BlockSchema schema, BlockData data) { }

    private void addContacts(SitePage page) {
        try (var input = new ClassPathResource("cms/contact-blocks.json").getInputStream()) {
            List<InitialBlock> initial = new ObjectMapper().readValue(input, new TypeReference<List<InitialBlock>>() { });
            for (InitialBlock source : initial) {
                SiteBlock block = new SiteBlock();
                block.page = page;
                block.key = source.key();
                block.title = source.title();
                block.type = source.type();
                block.schema = source.schema();
                block.data = source.data();
                page.blocks.add(block);
            }
        } catch (java.io.IOException error) {
            throw new IllegalStateException("Не удалось загрузить блоки контактов", error);
        }
    }

    private void addPolicy(SitePage page, String text) {
        var matcher = java.util.regex.Pattern.compile("(?m)^(\\d{1,2})\\. ([^\\n]+)$").matcher(text);
        List<String> titles = new ArrayList<>();
        List<Integer> starts = new ArrayList<>();
        List<Integer> ends = new ArrayList<>();
        while (matcher.find()) {
            titles.add(matcher.group());
            starts.add(matcher.start());
            ends.add(matcher.end());
        }
        if (titles.isEmpty()) throw new IllegalStateException("Не найдены разделы исходной политики");
        for (int index = 0; index < titles.size(); index++) {
            SiteBlock block = new SiteBlock();
            block.page = page;
            block.key = "policy-section-" + (index + 1);
            block.title = titles.get(index);
            block.type = BlockType.COPY;
            block.schema = new BlockSchema(Map.of("title", new FieldRule("Заголовок раздела", "TEXT"), "body", new FieldRule("Текст раздела", "TEXT")), Map.of());
            String body = text.substring(ends.get(index), index + 1 < titles.size() ? starts.get(index + 1) : text.length()).trim();
            block.data = new BlockData(Map.of("title", new Value(block.title, null, null), "body", new Value(body, null, null)), List.of());
            page.blocks.add(block);
        }
    }

    private void migrateCases() {
        groups("project-cards").forEach((group, fields) -> {
            CaseStudy project = new CaseStudy();
            common(project, Kind.CASES, group, fields, "title", "description", "image");
            String href = href(fields, "details");
            if (href != null && href.startsWith("/cases/")) project.slug = href.substring("/cases/".length());
            project.body = new CaseContent(null, null, null, text(fields, "description", null), null, List.of(), List.of(), List.of());
            for (Map<String, ContentItem> relation : groups("cases-page-items").values()) {
                if (group.equals(text(relation, "projectGroup", null))) addFilters(project, relation, "cases-page-filters");
            }
            if (project.categories.isEmpty()) {
                String label = text(fields, "category", "");
                String categorySlug = switch (label) {
                    case "Web разработка" -> "web";
                    case "Mobile разработка" -> "mobile";
                    case "Цифровая автоматизация" -> "automation";
                    case "Интеграция и разработка CRM систем" -> "crm";
                    default -> null;
                };
                if (categorySlug != null) project.categories.add(category(Kind.CASES, categorySlug, label));
            }
            projects.put(group, (CaseStudy) catalog.save(project));
        });
    }

    private void migrateServices() {
        Map<String, String> names = Map.of("web", "Web разработка", "mobile", "Mobile разработка", "automation", "Цифровая автоматизация",
                "crm", "Интеграция и разработка CRM систем", "1c", "Услуги в области 1С интеграций", "outstaff", "Аутстаффинг IT специалистов");
        groups("services-page-catalog").forEach((group, fields) -> {
            ServiceOffering service = new ServiceOffering();
            common(service, Kind.SERVICES, group, fields, "title", "description", null);
            String categoryKey = group.contains("-service-") ? group.substring(0, group.indexOf("-service-")) : "other";
            service.categories.add(category(Kind.SERVICES, categoryKey, text(fields, "category", names.getOrDefault(categoryKey, "Другое"))));
            String[] mobileSlugs = {"mobile-android", "mobile-ios", "mobile-cross", "mobile-pwa", "mobile-support"};
            for (int index = 0; index < mobileSlugs.length; index++) if (group.equals("mobile-service-" + (index + 1))) service.slug = mobileSlugs[index];
            Map<String, ContentItem> stack = groups("services-page-stacks").getOrDefault(categoryKey, Map.of());
            List<Technology> technologies = new ArrayList<>();
            groups("services-page-stack-items").forEach((key, technology) -> {
                if (key.startsWith(categoryKey + "-technology-")) {
                    MediaAsset image = image(technology, "logo");
                    technologies.add(new Technology(text(technology, "name", key), image == null ? null : image.getId()));
                    if (image != null) service.images.add(image);
                }
            });
            if (technologies.isEmpty()) {
                String fallbackCategory = categoryKey.equals("outstaff") ? "mobile" : "automation";
                groups("services-page-stack-items").forEach((key, technology) -> {
                    if (key.startsWith(fallbackCategory + "-technology-")) {
                        MediaAsset image = image(technology, "logo");
                        technologies.add(new Technology(text(technology, "name", key), image == null ? null : image.getId()));
                        if (image != null) service.images.add(image);
                    }
                });
            }
            Set<String> selectedStack = switch (service.slug) {
                case "mobile-android" -> Set.of("Java", "Kotlin");
                case "mobile-ios" -> Set.of("Swift", "Objective-C");
                case "mobile-cross" -> Set.of("Flutter");
                case "mobile-pwa" -> Set.of("HTML5", "HTML", "CSS", "JavaScript");
                default -> Set.of();
            };
            if (!selectedStack.isEmpty()) technologies.removeIf(technology -> !selectedStack.contains(technology.name()));
            String stackTitle = selectedStack.isEmpty() ? text(stack, "title", "Технологии") : service.title;
            String stackDescription = selectedStack.isEmpty() ? text(stack, "description", null) : service.description;
            if (service.slug.equals("mobile-cross") && "Разработка кроссплатформенного мобильного приложения на одной кодовой базе.".equals(stackDescription))
                stackDescription = "Разработка кроссплатформенного мобильного приложения";
            List<Faq> faq = groups("faq-items").values().stream().map(row -> new Faq(text(row, "question", "Вопрос"), text(row, "answer", ""))).toList();
            service.body = new ServiceContent(single("services-page-projects", "title"), "Частые вопросы",
                    stackTitle, stackDescription, single("services-page-process", "title"),
                    single("services-page-tasks", "title"), single("services-page-audience", "title"), single("services-page-audience", "eyebrow"),
                    single("services-page-cooperation", "title"), faq, technologies, items("services-page-process-steps"),
                    items("services-page-tasks"), items("services-page-audiences"), items("services-page-cooperation-models"));
            service.examples.addAll(linkedProjects("services-page-project-items"));
            services.put(group, (ServiceOffering) catalog.save(service));
        });
    }

    private void migrateCaseDetails() {
        try (var input = new ClassPathResource("cms/case-details.json").getInputStream()) {
            Map<String, CaseContent> details = new ObjectMapper().readValue(input, new TypeReference<Map<String, CaseContent>>() { });
            projects.values().forEach(project -> {
                if (details.containsKey(project.slug)) project.body = details.get(project.slug);
            });
        } catch (java.io.IOException error) {
            throw new IllegalStateException("Не удалось загрузить содержимое кейсов", error);
        }
    }

    private void migratePeople() {
        groups("outstaff-page-specialist-items").forEach((group, fields) -> {
            OutstaffSpecialist specialist = new OutstaffSpecialist();
            common(specialist, Kind.SPECIALISTS, group, fields, "title", "description", "photo");
            String skills = text(fields, "skills", "");
            List<String> grades = fields.entrySet().stream().filter(field -> field.getKey().matches("grade\\d*"))
                    .map(field -> field.getValue().getValue()).filter(Objects::nonNull).filter(value -> !value.isBlank()).toList();
            specialist.body = new SpecialistContent(text(fields, "direction", null), grades, null, null,
                    Arrays.stream(skills.split("[•\\n]")).map(String::trim).filter(value -> !value.isEmpty()).toList());
            addFilters(specialist, fields, "outstaff-page-filters");
            specialists.put(group, (OutstaffSpecialist) catalog.save(specialist));
        });
        groups("team-members").forEach((group, fields) -> {
            TeamMember member = new TeamMember();
            common(member, Kind.TEAM, group, fields, "name", "description", "photo");
            member.body = new MemberContent(text(fields, "position", null), null);
            if (group.equals("member-6")) {
                if (member.title.equals("Алёна")) member.title = "Алёша";
                if ("Разработчик".equals(member.body.position())) member.body = new MemberContent("React-разработчик", member.body.biography());
            }
            team.put(group, (TeamMember) catalog.save(member));
        });
    }

    private void migrateHomeSpecialists(SitePage page) {
        List<OutstaffSpecialist> selected = new ArrayList<>();
        groups("staffing-cards").forEach((group, fields) -> {
            String title = text(fields, "title", "");
            List<String> skills = Arrays.stream(text(fields, "stack", "").split("[•\\n]"))
                    .map(String::trim).filter(value -> !value.isEmpty()).toList();
            OutstaffSpecialist specialist = specialists.values().stream()
                    .filter(entry -> entry.title.equals(title) && entry.body.skills().equals(skills)).findFirst().orElse(null);
            if (specialist == null && group.equals("role-1")) specialist = specialists.get("specialist-1");
            if (specialist == null) {
                specialist = new OutstaffSpecialist();
                common(specialist, Kind.SPECIALISTS, group, fields, "title", "description", null);
                String direction = text(fields, "tag1", "").replaceFirst("^#\\s*", "");
                List<String> grades = fields.entrySet().stream().filter(field -> field.getKey().matches("tag[23]"))
                        .map(field -> field.getValue().getValue()).filter(Objects::nonNull)
                        .map(value -> value.replaceFirst("^#\\s*", "")).toList();
                specialist.body = new SpecialistContent(direction, grades, null, null, skills);
                specialist = (OutstaffSpecialist) catalog.save(specialist);
            }
            selected.add(specialist);
        });
        PageCollection binding = collection(page, "specialists", Kind.SPECIALISTS, false, selected);
        OutstaffSpecialist designer = specialists.get("specialist-1");
        if (designer != null && selected.contains(designer)) binding.overrides.add(new DisplayOverride(designer.id, null, null, null, null,
                new SpecialistContent(designer.body.direction(), List.of("Middle", "Senior"), designer.body.experience(),
                        designer.body.availability(), designer.body.skills())));
    }

    private void common(CatalogEntry entry, Kind kind, String slug, Map<String, ContentItem> fields, String title, String description, String cover) {
        entry.kind = kind;
        entry.slug = slug;
        entry.title = text(fields, title, slug);
        entry.description = text(fields, description, null);
        entry.price = text(fields, "price", null);
        entry.published = fields.values().stream().allMatch(ContentItem::isPublished);
        entry.sortOrder = fields.values().stream().mapToInt(ContentItem::getSortOrder).min().orElse(0);
        entry.cover = cover == null ? null : image(fields, cover);
        entry.mobileCover = image(fields, "imageMobile");
        fields.values().stream().map(ContentItem::getMedia).filter(Objects::nonNull).forEach(entry.images::add);
    }

    private void addFilters(CatalogEntry entry, Map<String, ContentItem> fields, String filterSection) {
        fields.forEach((key, item) -> {
            if (!key.matches("filter\\d+")) return;
            String slug = item.getValue();
            if (slug == null || slug.equals("all")) return;
            String label = groups(filterSection).values().stream().filter(row -> slug.equals(text(row, "key", null)))
                    .map(row -> text(row, "label", slug)).findFirst().orElse(slug);
            entry.categories.add(category(entry.kind, slug, label));
        });
    }
    private CatalogCategory category(Kind kind, String slug, String name) {
        return categoryIndex.computeIfAbsent(kind + ":" + slug, ignored -> {
            CatalogCategory category = new CatalogCategory();
            category.kind = kind;
            category.slug = slug;
            category.name = name;
            category.sortOrder = categoryIndex.size();
            return categories.save(category);
        });
    }
    private List<CaseStudy> linkedProjects(String section) {
        return groups(section).values().stream().map(fields -> projects.get(text(fields, "projectGroup", null)))
                .filter(Objects::nonNull).distinct().toList();
    }
    private PageCollection collection(SitePage page, String key, Kind kind, boolean all, List<? extends CatalogEntry> entries) {
        PageCollection collection = new PageCollection();
        collection.page = page;
        collection.key = key;
        collection.kind = kind;
        collection.allEntries = all;
        collection.entries.addAll(entries);
        page.collections.add(collection);
        return collection;
    }
    private SiteBlock block(SitePage page, String section, List<ContentItem> items) {
        SiteBlock block = new SiteBlock();
        block.page = page;
        block.key = section;
        block.sortOrder = page.blocks.size();
        block.title = blockTitle(section);
        block.type = section.contains("seo") ? BlockType.SEO : section.contains("contact") ? BlockType.FORM
                : section.contains("header") || section.contains("footer") ? BlockType.NAVIGATION
                : section.contains("faq") ? BlockType.FAQ : items.stream().anyMatch(item -> item.getGroupName() != null) ? BlockType.CARDS
                : section.endsWith("-page") || section.equals("hero") ? BlockType.HERO : BlockType.COPY;
        Map<String, FieldRule> fieldSchema = new LinkedHashMap<>();
        Map<String, FieldRule> rowSchema = new LinkedHashMap<>();
        Map<String, Value> fields = new LinkedHashMap<>();
        Map<String, Map<String, Value>> rows = new LinkedHashMap<>();
        for (ContentItem item : items) {
            Map<String, FieldRule> schema = item.getGroupName() == null ? fieldSchema : rowSchema;
            schema.putIfAbsent(item.getItemKey(), new FieldRule(fieldTitle(item), item.getType()));
            Map<String, Value> values = item.getGroupName() == null ? fields : rows.computeIfAbsent(item.getGroupName(), ignored -> new LinkedHashMap<>());
            String link = item.getHref();
            if ("#contact".equals(link)) link = page.slug.equals("shared") ? "/#lead-form" : page.path + "#lead-form";
            if (item.getItemKey().equals("all")) {
                if ("#services".equals(link)) link = "/services";
                if ("#projects".equals(link)) link = "/cases";
                if ("#staffing".equals(link)) link = "/outstaff";
            }
            values.put(item.getItemKey(), new Value(item.getValue(), link, item.getMedia() == null ? null : item.getMedia().getId(), item.isPublished()));
            if (item.getMedia() != null) block.images.add(item.getMedia());
        }
        if (Set.of("services-page", "services-page-stacks", "services-page-stack-items").contains(section) && !rows.isEmpty()) {
            rowSchema.put("category", new FieldRule("Категория услуг (slug)", "TEXT"));
            rows.forEach((group, row) -> {
                String categorySlug = group.contains("-technology-") ? group.substring(0, group.indexOf("-technology-")) : group;
                row.put("category", new Value(categorySlug, null, null, row.values().stream().anyMatch(Value::visible)));
            });
        }
        if (section.equals("technology-tabs")) {
            rowSchema.put("key", new FieldRule("Ключ направления", "TEXT"));
            rowSchema.put("description", new FieldRule("Описание направления", "TEXT"));
            String[] keys = {"backend", "frontend", "mobile", "infra", "testing", "analytics"};
            String[] descriptions = {single("technologies", "description"),
                    "Собираем быстрые и доступные интерфейсы: от лендингов до личных кабинетов и сложных SPA.",
                    "Нативные и кроссплатформенные приложения для iOS и Android с одинаковым качеством на всех устройствах.",
                    "Выстраиваем CI/CD, контейнеризацию, мониторинг и отказоустойчивую инфраструктуру под нагрузку.",
                    "Автоматизируем регрессионное, нагрузочное и сквозное тестирование, чтобы релизы не ломали прод.",
                    "Собираем данные, строим витрины и дашборды — чтобы решения принимались на цифрах, а не на ощущениях."};
            for (int index = 0; index < keys.length; index++) {
                Map<String, Value> row = rows.get("tab-" + (index + 1));
                if (row == null) continue;
                boolean visible = row.values().stream().anyMatch(Value::visible);
                row.putIfAbsent("key", new Value(keys[index], null, null, visible));
                row.putIfAbsent("description", new Value(descriptions[index], null, null, visible));
            }
        }
        if (section.equals("team")) {
            fieldSchema.putIfAbsent("moreCount", new FieldRule("Число остальных специалистов", "TEXT"));
            fieldSchema.putIfAbsent("moreText", new FieldRule("Подпись остальных специалистов", "TEXT"));
            fields.putIfAbsent("moreCount", new Value("70+", null, null));
            fields.putIfAbsent("moreText", new Value("И еще более 70 крутых специалистов в команде", null, null));
        }
        rows.values().forEach(row -> rowSchema.keySet().forEach(key -> row.putIfAbsent(key, new Value(null, null, null, false))));
        block.schema = new BlockSchema(fieldSchema, rowSchema);
        block.data = new BlockData(fields, new ArrayList<>(rows.values()));
        block.published = items.stream().anyMatch(ContentItem::isPublished);
        return block;
    }
    private String blockTitle(String section) {
        String title = single(section, "title");
        if (title != null && !title.isBlank()) return title.substring(0, Math.min(title.length(), 240));
        Map<String, String> suffixes = Map.ofEntries(
                Map.entry("-title-words", "Сменяемые слова заголовка"), Map.entry("-process-steps", "Этапы работы"),
                Map.entry("-stack-items", "Список технологий"), Map.entry("-stacks", "Стек по направлениям"),
                Map.entry("-audiences", "Аудитория услуги"), Map.entry("-cooperation-models", "Модели сотрудничества"),
                Map.entry("-seo-topics", "Темы SEO-блока"), Map.entry("-seo-items", "Пункты SEO-блока"),
                Map.entry("-seo-links", "Ссылки SEO-блока"), Map.entry("-use-case-items", "Сценарии применения"),
                Map.entry("-advantage-items", "Список преимуществ"), Map.entry("-stage-items", "Этапы проекта"),
                Map.entry("-audience-items", "Для кого предназначен продукт"), Map.entry("-control-items", "Возможности управления"),
                Map.entry("-term-items", "Условия сотрудничества"), Map.entry("-contact-specialists", "Варианты выбора специалиста в форме"),
                Map.entry("-contact-grades", "Варианты выбора грейда в форме"));
        for (Map.Entry<String, String> suffix : suffixes.entrySet()) if (section.endsWith(suffix.getKey())) return suffix.getValue();
        return switch (section) {
            case "header" -> "Шапка сайта";
            case "footer" -> "Подвал сайта";
            case "footer-navigation" -> "Навигация в подвале";
            case "footer-services" -> "Услуги в подвале";
            case "footer-regions" -> "Регионы в подвале";
            case "team-members" -> "Команда";
            case "brands" -> "Клиенты и партнёры";
            case "stat-cards" -> "Показатели компании";
            case "technology-tabs" -> "Направления технологий";
            case "technology-items" -> "Список технологий";
            case "seo-services" -> "Услуги в SEO-блоке";
            default -> section;
        };
    }
    private String fieldTitle(ContentItem item) {
        if (item.getItemKey().matches("nav\\d+")) return "Пункт меню " + item.getItemKey().substring(3);
        if (item.getItemKey().matches("paragraph\\d+")) return "Абзац " + item.getItemKey().substring(9);
        return switch (item.getItemKey()) {
            case "title" -> "Заголовок";
            case "description" -> "Описание";
            case "eyebrow" -> "Надзаголовок";
            case "name" -> "Название";
            case "price" -> "Стоимость";
            case "image", "photo" -> "Изображение";
            case "question" -> "Вопрос";
            case "answer" -> "Ответ";
            case "word" -> "Слово";
            case "key" -> "Значение варианта";
            case "label" -> "Подпись";
            case "number" -> "Число или номер";
            case "tag", "tag1", "tag2", "tag3" -> "Текстовая метка";
            case "link", "cta", "details" -> "Ссылка";
            default -> item.getLabel() == null ? item.getItemKey() : item.getLabel();
        };
    }
    private Map<String, Map<String, ContentItem>> groups(String section) {
        Map<String, Map<String, ContentItem>> result = new LinkedHashMap<>();
        sections.getOrDefault(section, List.of()).stream().filter(item -> item.getGroupName() != null)
                .forEach(item -> result.computeIfAbsent(item.getGroupName(), ignored -> new LinkedHashMap<>()).put(item.getItemKey(), item));
        return result;
    }
    private Map<String, ContentItem> fields(List<ContentItem> items) {
        Map<String, ContentItem> result = new LinkedHashMap<>();
        items.forEach(item -> result.put(item.getItemKey(), item));
        return result;
    }
    private String single(String section, String key) {
        return sections.getOrDefault(section, List.of()).stream().filter(item -> item.getGroupName() == null && item.getItemKey().equals(key))
                .map(item -> item.getValue() == null ? "" : item.getValue()).findFirst().orElse(null);
    }
    private String text(Map<String, ContentItem> fields, String key, String fallback) {
        ContentItem item = fields.get(key);
        return item == null || item.getValue() == null ? fallback : item.getValue();
    }
    private String href(Map<String, ContentItem> fields, String key) { return fields.containsKey(key) ? fields.get(key).getHref() : null; }
    private MediaAsset image(Map<String, ContentItem> fields, String key) { return fields.containsKey(key) ? fields.get(key).getMedia() : null; }
    private List<Item> items(String section) {
        return groups(section).values().stream().map(row -> new Item(text(row, "title", text(row, "name", "")), text(row, "description", null), text(row, "price", null))).toList();
    }
}
