package ru.itwebs.cms.catalog;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.itwebs.cms.blog.BlogConflictException;
import ru.itwebs.cms.entity.*;
import ru.itwebs.cms.repository.*;
import ru.itwebs.cms.service.StructuredPayloadLimits;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import static ru.itwebs.cms.catalog.ServiceComponentDtos.*;
import static ru.itwebs.cms.catalog.CatalogDtos.*;

@Service
@Transactional(readOnly = true)
public class ServiceComponentService {
    private final ServiceComponentRepository components;
    private final ServiceComponentLinkRepository links;
    private final MediaRepository media;
    private final ObjectMapper mapper;
    private final StructuredPayloadLimits limits;

    public ServiceComponentService(ServiceComponentRepository components, ServiceComponentLinkRepository links,
                                   MediaRepository media, ObjectMapper mapper, StructuredPayloadLimits limits) {
        this.components = components;
        this.links = links;
        this.media = media;
        this.mapper = mapper;
        this.limits = limits;
    }

    public static ComponentKind kind(String value) {
        try { return ComponentKind.valueOf(value.toUpperCase(Locale.ROOT)); }
        catch (IllegalArgumentException error) { throw new NoSuchElementException("Справочник не найден"); }
    }

    public ComponentPage list(ComponentKind kind, int page, int size) {
        if (page < 0 || page > 100000 || size < 1 || size > 100) throw new IllegalArgumentException("page: 0–100000, size: 1–100");
        var result = components.findByKind(kind, PageRequest.of(page, size, Sort.by("sortOrder", "id")));
        return new ComponentPage(result.getContent().stream().map(this::view).toList(), page, size, result.getTotalElements(), result.getTotalPages());
    }

    public ComponentView get(ComponentKind kind, Long id) { return view(find(kind, id)); }

    private ServiceComponent find(ComponentKind kind, Long id) {
        return components.findById(id).filter(item -> item.kind == kind).orElseThrow(() -> new NoSuchElementException("Элемент справочника не найден"));
    }

    @Transactional
    public ComponentView save(ComponentKind kind, Long id, ComponentRequest request) {
        limits.validate(request);
        ServiceComponent item = id == null ? new ServiceComponent() : components.findByIdForUpdate(id)
                .filter(value -> value.kind == kind).orElseThrow(() -> new NoSuchElementException("Элемент справочника не найден"));
        if (id != null) CatalogService.version(item.version, request.version());
        item.kind = kind;
        item.title = request.title().trim();
        item.description = request.description();
        item.price = request.price();
        if (request.label() != null) {
            validateLabel(kind, request.label());
            item.label = request.label().isBlank() ? null : request.label().trim();
        }
        validateContent(kind, item.title, item.description, item.price);
        if (request.mediaId() != null && kind != ComponentKind.STACK) throw new IllegalArgumentException("Логотип допустим только у технологии");
        item.image = request.mediaId() == null ? null : ru.itwebs.cms.service.MediaFormats.requireImage(media.findByIdForUpdate(request.mediaId())
                .orElseThrow(() -> new IllegalArgumentException("Изображение не найдено")));
        item.sortOrder = request.sortOrder();
        item.fingerprint = fingerprint(item);
        ServiceComponent duplicate = components.findByFingerprint(item.fingerprint).orElse(null);
        if (duplicate != null && !Objects.equals(id, duplicate.id)) throw new BlogConflictException("Такой элемент уже есть в справочнике");
        return view(components.saveAndFlush(item));
    }

    @Transactional
    public void delete(ComponentKind kind, Long id) {
        ServiceComponent item = find(kind, id);
        if (links.existsByComponentId(id)) throw new BlogConflictException("Элемент выбран в услуге. Сначала уберите его из услуг");
        components.delete(item);
        components.flush();
    }

    private ComponentView view(ServiceComponent item) {
        MediaView image = item.image == null ? null : new MediaView(item.image.getId(), "/api/media/" + item.image.getId(), item.image.getAltText());
        return new ComponentView(item.id, item.version, item.kind, item.title, item.description, item.price, image, item.sortOrder, item.label);
    }

    private void validateLabel(ComponentKind kind, String label) {
        if (kind != ComponentKind.TASK && kind != ComponentKind.PROCESS)
            throw new IllegalArgumentException("Бейдж допустим только для задачи или этапа");
        if (label.length() > 240) throw new IllegalArgumentException("Бейдж: до 240 символов");
    }

    private void validateContent(ComponentKind kind, String title, String description, String price) {
        int maximum = kind == ComponentKind.FAQ ? 1000 : kind == ComponentKind.STACK ? 120 : 500;
        if (title == null || title.isBlank() || title.length() > maximum) throw new IllegalArgumentException("Название: от 1 до " + maximum + " символов");
        if (description != null && description.length() > 20000) throw new IllegalArgumentException("Описание: до 20000 символов");
        if (price != null && price.length() > 240) throw new IllegalArgumentException("Стоимость: до 240 символов");
        if (kind == ComponentKind.FAQ && (description == null || description.isBlank())) throw new IllegalArgumentException("Заполните ответ FAQ");
        if (kind == ComponentKind.STACK && (description != null || price != null)) throw new IllegalArgumentException("У технологии задаются название и логотип");
        if (kind == ComponentKind.FAQ && price != null) throw new IllegalArgumentException("Стоимость не относится к FAQ");
    }

    private String fingerprint(ServiceComponent item) {
        try {
            List<Object> values = new ArrayList<>(Arrays.asList(item.kind, item.title, item.description, item.price,
                    item.image == null ? null : item.image.getId()));
            // Preserve existing fingerprints when no label has been configured.
            if (item.label != null) values.add(item.label);
            String value = mapper.writeValueAsString(values);
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (JsonProcessingException | NoSuchAlgorithmException error) {
            throw new IllegalStateException("Не удалось обработать элемент справочника", error);
        }
    }

    private Map<ComponentKind, List<Selection>> grouped(Selections selected) {
        Map<ComponentKind, List<Selection>> result = new LinkedHashMap<>();
        result.put(ComponentKind.FAQ, selected.faq());
        result.put(ComponentKind.STACK, selected.stack());
        result.put(ComponentKind.PROCESS, selected.process());
        result.put(ComponentKind.TASK, selected.tasks());
        result.put(ComponentKind.AUDIENCE, selected.audience());
        result.put(ComponentKind.COOPERATION, selected.cooperation());
        return result;
    }

    @Transactional
    public void replace(ServiceOffering service, Selections selected) {
        replaceSelected(service, selected);
    }

    private void replaceSelected(ServiceOffering service, Selections selected) {
        if (selected == null) throw new IllegalArgumentException("Выберите элементы справочников в serviceSelections");
        limits.validate(selected);
        Map<ComponentKind, List<Selection>> groups = grouped(selected);
        Map<Long, ServiceComponent> chosen = new TreeMap<>();
        for (List<Selection> values : groups.values()) for (Selection value : values) chosen.put(value.componentId(), null);
        for (Long id : chosen.keySet()) chosen.put(id, components.findByIdForUpdate(id)
                .orElseThrow(() -> new IllegalArgumentException("Элемент справочника не найден: " + id)));
        Map<Long, Deque<ServiceComponentLink>> existing = new HashMap<>();
        service.components.forEach(link -> existing.computeIfAbsent(link.component.id, ignored -> new ArrayDeque<>()).add(link));
        Set<ServiceComponentLink> retained = new HashSet<>();
        int position = 0;
        for (var group : groups.entrySet()) for (Selection value : group.getValue()) {
            ServiceComponent component = chosen.get(value.componentId());
            if (component.kind != group.getKey()) throw new IllegalArgumentException("Элемент относится к другому справочнику");
            ComponentOverride override = value.override();
            if (override != null && override.label() != null) validateLabel(component.kind, override.label());
            validateContent(component.kind, effective(override == null ? null : override.title(), component.title),
                    effective(override == null ? null : override.description(), component.description),
                    effective(override == null ? null : override.price(), component.price));
            Deque<ServiceComponentLink> candidates = existing.get(component.id);
            ServiceComponentLink link = candidates == null ? null : candidates.pollFirst();
            if (link == null) {
                link = new ServiceComponentLink();
                link.service = service;
                link.component = component;
                service.components.add(link);
            }
            link.position = position++;
            link.override = override;
            retained.add(link);
        }
        service.components.removeIf(link -> !retained.contains(link));
        service.componentsRevision = service.componentsRevision == null ? 1 : service.componentsRevision + 1;
        service.body = headers(service.body);
    }

    public Selections selections(ServiceOffering service) {
        return new Selections(selected(service, ComponentKind.FAQ), selected(service, ComponentKind.STACK),
                selected(service, ComponentKind.PROCESS), selected(service, ComponentKind.TASK),
                selected(service, ComponentKind.AUDIENCE), selected(service, ComponentKind.COOPERATION));
    }

    private List<ServiceComponentLink> ordered(ServiceOffering service, ComponentKind kind) {
        return service.components.stream().filter(link -> link.component.kind == kind)
                .sorted(Comparator.comparingInt(link -> link.position)).toList();
    }

    private List<Selection> selected(ServiceOffering service, ComponentKind kind) {
        return ordered(service, kind).stream().map(link -> new Selection(link.component.id, link.override)).toList();
    }

    private String effective(String override, String original) { return override == null ? original : override; }
    private String title(ServiceComponentLink link) { return effective(link.override == null ? null : link.override.title(), link.component.title); }
    private String description(ServiceComponentLink link) { return effective(link.override == null ? null : link.override.description(), link.component.description); }

    public ServiceContent resolve(ServiceOffering service) {
        ServiceContent body = service.body;
        return new ServiceContent(body.examplesTitle(), body.faqTitle(), body.stackTitle(), body.stackDescription(), body.processTitle(),
                body.tasksTitle(), body.audienceTitle(), body.audienceDescription(), body.cooperationTitle(),
                ordered(service, ComponentKind.FAQ).stream().map(link -> new Faq(title(link), description(link))).toList(),
                ordered(service, ComponentKind.STACK).stream().map(link -> new Technology(title(link), link.component.image == null ? null : link.component.image.getId())).toList(),
                items(service, ComponentKind.PROCESS), items(service, ComponentKind.TASK), items(service, ComponentKind.AUDIENCE), items(service, ComponentKind.COOPERATION));
    }

    private List<Item> items(ServiceOffering service, ComponentKind kind) {
        return ordered(service, kind).stream().map(link -> new Item(title(link), description(link),
                effective(link.override == null ? null : link.override.price(), link.component.price),
                effective(link.override == null ? null : link.override.label(), link.component.label))).toList();
    }

    public List<MediaAsset> images(ServiceOffering service) {
        return service.components.stream().map(link -> link.component.image).filter(Objects::nonNull).toList();
    }

    private ServiceContent headers(ServiceContent body) {
        return new ServiceContent(body.examplesTitle(), body.faqTitle(), body.stackTitle(), body.stackDescription(), body.processTitle(),
                body.tasksTitle(), body.audienceTitle(), body.audienceDescription(), body.cooperationTitle(),
                List.of(), List.of(), List.of(), List.of(), List.of(), List.of());
    }

    @Transactional
    public void migrate(ServiceOffering service) {
        ServiceContent body = service.body;
        if (body == null) throw new IllegalStateException("Услуга не содержит исходного контента: " + service.id);
        Selections selected = new Selections(
                body.faq().stream().map(item -> imported(ComponentKind.FAQ, item.question(), item.answer(), null, null)).toList(),
                body.stack().stream().map(item -> imported(ComponentKind.STACK, item.name(), null, null, item.mediaId())).toList(),
                importedItems(ComponentKind.PROCESS, body.process()), importedItems(ComponentKind.TASK, body.tasks()),
                importedItems(ComponentKind.AUDIENCE, body.audience()), importedItems(ComponentKind.COOPERATION, body.cooperation()));
        replace(service, selected);
        service.images.removeIf(image -> (service.cover == null || !service.cover.getId().equals(image.getId()))
                && (service.mobileCover == null || !service.mobileCover.getId().equals(image.getId())));
    }

    private List<Selection> importedItems(ComponentKind kind, List<Item> items) {
        return items.stream().map(item -> imported(kind, item.title(), item.description(), item.price(), null, item.label())).toList();
    }

    private Selection imported(ComponentKind kind, String title, String description, String price, Long mediaId) {
        return imported(kind, title, description, price, mediaId, null);
    }

    private Selection imported(ComponentKind kind, String title, String description, String price, Long mediaId, String label) {
        ServiceComponent item = new ServiceComponent();
        item.label = label;
        item.kind = kind;
        item.title = title;
        item.description = description;
        item.price = price;
        item.image = mediaId == null ? null : ru.itwebs.cms.service.MediaFormats.requireImage(media.findByIdForUpdate(mediaId).orElseThrow());
        item.fingerprint = fingerprint(item);
        ServiceComponent existing = components.findByFingerprint(item.fingerprint).orElse(null);
        if (existing == null) {
            item.sortOrder = (int) Math.min(components.count(), Integer.MAX_VALUE);
            existing = components.saveAndFlush(item);
        }
        return new Selection(existing.id, null);
    }
}
