package ru.itwebs.cms.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.itwebs.cms.service.AmoCrmIntegrationService;

@RestController
@RequestMapping("/api/admin/integrations/amocrm")
@Tag(name = "Админ: amoCRM", description = "Подключение внешней интеграции amoCRM")
@SecurityRequirement(name = "basicAuth")
public class AdminAmoCrmController {
    private final AmoCrmIntegrationService amoCrm;

    public AdminAmoCrmController(AmoCrmIntegrationService amoCrm) { this.amoCrm = amoCrm; }

    @GetMapping(value = "/authorize", produces = MediaType.TEXT_HTML_VALUE)
    @Operation(summary = "Открыть  подключение amoCRM", description = "Откройте в браузере с Basic Auth администратора, затем нажмите кнопку подключения.")
    public String authorize() { return amoCrm.createAuthorizationPage(); }
}
