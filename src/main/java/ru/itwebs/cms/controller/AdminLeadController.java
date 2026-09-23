package ru.itwebs.cms.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.itwebs.cms.dto.LeadResponse;
import ru.itwebs.cms.dto.LeadStatusRequest;
import ru.itwebs.cms.service.LeadService;
import java.util.List;

@RestController
@RequestMapping("/api/admin/leads")
@Tag(name = "Админ: заявки", description = "Просмотр, обработка и удаление заявок")
@SecurityRequirement(name = "basicAuth")
public class AdminLeadController {
    private final LeadService leads;
    public AdminLeadController(LeadService leads) { this.leads = leads; }

    @GetMapping
    @Operation(summary = "Список заявок", description = "В обратном хронологическом порядке.")
    public List<LeadResponse> list() { return leads.list().stream().map(LeadResponse::from).toList(); }

    @GetMapping("/{id}")
    @Operation(summary = "Заявка по ID")
    public LeadResponse get(@PathVariable Long id) { return LeadResponse.from(leads.find(id)); }

    @PatchMapping("/{id}")
    @Operation(summary = "Изменить статус заявки")
    public LeadResponse setProcessed(@PathVariable Long id, @Valid @RequestBody LeadStatusRequest request) {
        leads.setProcessed(id, request.processed());
        return LeadResponse.from(leads.find(id));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Удалить заявку")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        leads.delete(id);
        return ResponseEntity.noContent().build();
    }
}
