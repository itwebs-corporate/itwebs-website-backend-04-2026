package ru.itwebs.cms.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.itwebs.cms.dto.LeadRequest;
import ru.itwebs.cms.service.LeadService;
import java.net.URI;
import java.util.Map;

@RestController
@RequestMapping("/api/leads")
@Tag(name = "Публичные заявки", description = "Приём заявок с сайта")
public class LeadController {
    private final LeadService leads;
    public LeadController(LeadService leads) { this.leads = leads; }

    @PostMapping
    @Operation(summary = "Отправить заявку", description = "Требуются имя, задача, согласие и хотя бы один контакт: телефон или почта.")
    public ResponseEntity<Map<String, Long>> create(@Valid @RequestBody LeadRequest request) {
        Long id = leads.create(request).getId();
        return ResponseEntity.created(URI.create("/api/admin/leads/" + id)).body(Map.of("id", id));
    }
}
