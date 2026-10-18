package ru.itwebs.cms.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.itwebs.cms.catalog.ServiceComponentService;
import java.net.URI;
import static ru.itwebs.cms.catalog.ServiceComponentDtos.*;

@RestController
@RequestMapping("/api/admin/service-components/{kind}")
@SecurityRequirement(name = "basicAuth")
@Tag(name = "Админ: справочники услуг", description = "kind: faq, stack, process, task, audience, cooperation. Элементы создаются отдельно и выбираются в услугах. Общие изменения действуют во всех связанных услугах, кроме индивидуальных переопределений.")
public class AdminServiceComponentController {
    private final ServiceComponentService components;
    public AdminServiceComponentController(ServiceComponentService components) { this.components = components; }

    @GetMapping @Operation(summary = "Список элементов справочника")
    public ComponentPage list(@PathVariable String kind, @RequestParam(defaultValue = "0") int page,
                               @RequestParam(defaultValue = "20") int size) {
        return components.list(ServiceComponentService.kind(kind), page, size);
    }
    @GetMapping("/{id}") @Operation(summary = "Элемент справочника")
    public ComponentView get(@PathVariable String kind, @PathVariable Long id) {
        return components.get(ServiceComponentService.kind(kind), id);
    }
    @PostMapping @Operation(summary = "Создать элемент справочника")
    public ResponseEntity<ComponentView> create(@PathVariable String kind, @Valid @RequestBody ComponentRequest request) {
        ComponentView item = components.save(ServiceComponentService.kind(kind), null, request);
        return ResponseEntity.created(URI.create("/api/admin/service-components/" + kind + "/" + item.id())).body(item);
    }
    @PutMapping("/{id}") @Operation(summary = "Изменить общий элемент", description = "version из GET обязательна. Изменение обновляет все связанные услуги; индивидуальные переопределения сохраняются.")
    public ComponentView save(@PathVariable String kind, @PathVariable Long id, @Valid @RequestBody ComponentRequest request) {
        return components.save(ServiceComponentService.kind(kind), id, request);
    }
    @DeleteMapping("/{id}") @Operation(summary = "Удалить неиспользуемый элемент")
    public ResponseEntity<Void> delete(@PathVariable String kind, @PathVariable Long id) {
        components.delete(ServiceComponentService.kind(kind), id);
        return ResponseEntity.noContent().build();
    }
}
